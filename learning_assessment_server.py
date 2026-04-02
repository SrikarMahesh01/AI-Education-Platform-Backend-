from contextlib import asynccontextmanager
from datetime import datetime, timezone
import json
import logging
import os
from pathlib import Path
import re
import sqlite3
from typing import Any, Literal, Optional, TypeVar

import httpx
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from ollama import Client
from pydantic import BaseModel, Field, ValidationError

# Register the adapter
sqlite3.register_adapter(datetime, lambda x: x.isoformat())

DB_PATH = Path(__file__).resolve().with_name("learning_data.db")
SPRING_BOOT_API_BASE = os.getenv("SPRING_BOOT_API_BASE", "http://localhost:8084")
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "https://ollama.com").rstrip("/")
OLLAMA_API_KEY = os.getenv("OLLAMA_API_KEY")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL")
OLLAMA_DIRECT_CLOUD_MODEL_CANDIDATES = (
    "gpt-oss:20b",
    "gpt-oss:120b",
    "deepseek-v3.1:671b",
    "qwen3-coder:480b",
    "kimi-k2:1t",
)
OLLAMA_LOCAL_CLOUD_MODEL_CANDIDATES = (
    "gpt-oss:20b-cloud",
    "gpt-oss:120b-cloud",
    "deepseek-v3.1:671b-cloud",
    "qwen3-coder:480b-cloud",
    "kimi-k2:1t-cloud",
)
OLLAMA_LOCAL_MODEL_CANDIDATES_16GB = (
    "qwen3:8b",
    "qwen2.5:7b-instruct",
    "qwen2.5:7b",
    "llama3.3:8b",
    "llama3.2:3b-instruct",
    "llama3.1:8b-instruct",
    "gemma3:4b",
    "gemma2:9b",
    "mistral:7b",
    "llama3.2",
)
HTTP_TIMEOUT = httpx.Timeout(20.0, connect=5.0)
MODEL_DISCOVERY_TIMEOUT = httpx.Timeout(5.0, connect=3.0)
MODEL_SIZE_PATTERN = re.compile(r"(\d+(?:\.\d+)?)b", re.IGNORECASE)
TModel = TypeVar("TModel", bound=BaseModel)
_SELECTED_OLLAMA_MODEL: Optional[str] = None
_OLLAMA_CLIENT: Optional[Client] = None
try:
    OLLAMA_NUM_CTX = int(os.getenv("OLLAMA_NUM_CTX", "4096"))
except ValueError:
    OLLAMA_NUM_CTX = 4096

logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('server.log'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger(__name__)

def get_db_connection() -> sqlite3.Connection:
    """Create a SQLite connection with app defaults."""
    conn = sqlite3.connect(str(DB_PATH), detect_types=sqlite3.PARSE_DECLTYPES)
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def utc_now() -> datetime:
    """Return timezone-aware UTC datetime."""
    return datetime.now(timezone.utc)


def extract_model_size_b(model_name: str) -> Optional[float]:
    """Extract approximate model size in billions of parameters from model name."""
    match = MODEL_SIZE_PATTERN.search(model_name)
    if not match:
        return None
    return float(match.group(1))


def score_model_for_16gb(model_name: str) -> float:
    """Score model suitability for a 16GB RAM system."""
    normalized_name = model_name.lower()
    family_score = 50.0

    if "qwen3" in normalized_name:
        family_score = 120.0
    elif "qwen2.5" in normalized_name:
        family_score = 115.0
    elif "llama3.3" in normalized_name:
        family_score = 110.0
    elif "llama3.1" in normalized_name or "llama3.2" in normalized_name:
        family_score = 102.0
    elif "gemma3" in normalized_name:
        family_score = 98.0
    elif "gemma2" in normalized_name:
        family_score = 93.0
    elif "mistral" in normalized_name:
        family_score = 90.0

    size = extract_model_size_b(normalized_name)
    size_score = 0.0
    if size is not None:
        if size <= 10:
            size_score = 30.0 - abs(8.0 - size) * 4.0
        elif size <= 14:
            size_score = 8.0 - (size - 10.0) * 4.0
        else:
            size_score = -20.0

    quality_hints = 0.0
    if "instruct" in normalized_name:
        quality_hints += 6.0
    if "vision" in normalized_name:
        quality_hints -= 8.0

    return family_score + size_score + quality_hints


def get_ollama_client() -> Client:
    """Get or create Ollama client (cloud-first, local fallback via OLLAMA_HOST)."""
    global _OLLAMA_CLIENT
    if _OLLAMA_CLIENT is None:
        if OLLAMA_HOST.endswith("ollama.com") and not OLLAMA_API_KEY:
            raise RuntimeError(
                "OLLAMA_API_KEY is required when using Ollama Cloud host. "
                "Set OLLAMA_API_KEY or switch OLLAMA_HOST to a local Ollama server."
            )

        headers: dict[str, str] = {}
        if OLLAMA_API_KEY:
            headers["Authorization"] = f"Bearer {OLLAMA_API_KEY}"

        if headers:
            _OLLAMA_CLIENT = Client(host=OLLAMA_HOST, headers=headers)
        else:
            _OLLAMA_CLIENT = Client(host=OLLAMA_HOST)

    return _OLLAMA_CLIENT


def discover_ollama_models() -> list[str]:
    """Discover available Ollama models from configured host."""
    headers: dict[str, str] = {}
    if OLLAMA_API_KEY:
        headers["Authorization"] = f"Bearer {OLLAMA_API_KEY}"

    try:
        response = httpx.get(
            f"{OLLAMA_HOST}/api/tags",
            headers=headers or None,
            timeout=MODEL_DISCOVERY_TIMEOUT,
        )
        response.raise_for_status()
        payload = response.json()
        models = payload.get("models", [])
        return [m["name"] for m in models if isinstance(m, dict) and m.get("name")]
    except (httpx.HTTPError, ValueError) as e:
        logger.warning(f"Failed to discover Ollama models from {OLLAMA_HOST}: {str(e)}")
        return []


def resolve_ollama_model() -> str:
    """Pick best configured Ollama model with cloud-first preference."""
    if OLLAMA_MODEL:
        logger.info(f"Using OLLAMA_MODEL override: {OLLAMA_MODEL}")
        return OLLAMA_MODEL

    available_models = discover_ollama_models()
    if not available_models:
        fallback_model = "gpt-oss:20b"
        logger.warning(
            f"Could not discover models from {OLLAMA_HOST}; falling back to {fallback_model}. "
            "Set OLLAMA_MODEL to override."
        )
        return fallback_model

    available_lookup = {name.lower(): name for name in available_models}
    if OLLAMA_HOST.endswith("ollama.com"):
        for preferred in OLLAMA_DIRECT_CLOUD_MODEL_CANDIDATES:
            match = available_lookup.get(preferred.lower())
            if match:
                return match
    else:
        for preferred in OLLAMA_LOCAL_CLOUD_MODEL_CANDIDATES:
            match = available_lookup.get(preferred.lower())
            if match:
                return match

    for preferred in OLLAMA_LOCAL_MODEL_CANDIDATES_16GB:
        match = available_lookup.get(preferred.lower())
        if match:
            return match

    cloud_models = [m for m in available_models if "-cloud" in m.lower()]
    if cloud_models:
        return cloud_models[0]

    return max(available_models, key=score_model_for_16gb)


def get_ollama_model() -> str:
    """Return lazily resolved Ollama model."""
    global _SELECTED_OLLAMA_MODEL
    if _SELECTED_OLLAMA_MODEL is None:
        _SELECTED_OLLAMA_MODEL = resolve_ollama_model()
        logger.info(
            f"Selected Ollama model: {_SELECTED_OLLAMA_MODEL} on host {OLLAMA_HOST}"
        )
    return _SELECTED_OLLAMA_MODEL


def extract_json_payload(raw_content: str) -> str:
    """Extract pure JSON payload from model output."""
    payload = raw_content.strip()
    if payload.startswith("```"):
        payload = re.sub(r"^```(?:json)?\s*", "", payload, flags=re.IGNORECASE)
        payload = re.sub(r"\s*```$", "", payload).strip()

    start = payload.find("{")
    end = payload.rfind("}")
    if start != -1 and end != -1 and end > start:
        payload = payload[start : end + 1]
    return payload


def normalize_recommendations_payload(payload: dict[str, Any]) -> dict[str, Any]:
    """Normalize common model key variants to expected recommendations schema."""
    if "recommendations" in payload:
        return payload

    for alt_key in (
        "course_recommendations",
        "courseRecommendations",
        "recommended_courses",
        "recommendedCourses",
        "courses",
        "items",
        "results",
    ):
        if alt_key in payload and isinstance(payload[alt_key], list):
            payload = {**payload, "recommendations": payload[alt_key]}
            return payload

    return payload


def coerce_recommendations_payload(payload: dict[str, Any]) -> dict[str, Any]:
    """Coerce recommendation fields when model returns semantically similar keys."""
    normalized = normalize_recommendations_payload(payload)
    recs = normalized.get("recommendations")
    if not isinstance(recs, list):
        return normalized

    coerced_recs: list[dict[str, Any]] = []
    for item in recs:
        if not isinstance(item, dict):
            continue

        description = (
            item.get("description")
            or item.get("focus")
            or item.get("summary")
            or item.get("details")
            or f"Recommended learning option for {item.get('title', 'backend learning')}."
        )

        confidence_value = item.get("confidence_score")
        if confidence_value is None:
            rating = item.get("rating")
            if isinstance(rating, (int, float)):
                confidence_value = min(1.0, max(0.0, float(rating) / 5.0))
            else:
                confidence_value = 0.75

        coerced_recs.append(
            {
                "title": item.get("title", "Untitled recommendation"),
                "description": str(description),
                "confidence_score": float(confidence_value),
            }
        )

    return {**normalized, "recommendations": coerced_recs}


def coerce_string_list(value: Any) -> list[str]:
    """Normalize a value to a list of strings."""
    if value is None:
        return []
    if isinstance(value, list):
        return [str(v) for v in value if v is not None]
    if isinstance(value, str):
        return [value]
    return [str(value)]


def normalize_path_difficulty(value: Any) -> Literal["beginner", "intermediate", "advanced"]:
    """Normalize difficulty labels to learning-path schema values."""
    normalized = str(value).strip().lower()
    if normalized in {"beginner", "basic", "novice", "introductory", "intro"}:
        return "beginner"
    if normalized in {"advanced", "expert"}:
        return "advanced"
    return "intermediate"


def normalize_learning_path_payload(payload: dict[str, Any]) -> dict[str, Any]:
    """Normalize common model output variants to GeneratedLearningPath schema."""
    if isinstance(payload.get("learningPath"), dict):
        payload = {**payload, **payload["learningPath"]}

    if "modules" not in payload:
        for alt_key in (
            "learning_path",
            "learningPath",
            "course_path",
            "coursePath",
            "path",
            "phases",
            "stages",
        ):
            if isinstance(payload.get(alt_key), list):
                payload = {**payload, "modules": payload[alt_key]}
                break

    modules = payload.get("modules")
    if modules is None:
        for alt_key in ("plan", "roadmap"):
            if isinstance(payload.get(alt_key), list):
                modules = payload[alt_key]
                payload = {**payload, "modules": modules}
                break

    if not isinstance(modules, list):
        return payload

    processed_modules: list[dict[str, Any]] = []
    for module in modules:
        if not isinstance(module, dict):
            continue

        sub_modules = module.get("sub_modules")
        if not isinstance(sub_modules, list):
            for alt_key in ("subModules", "submodules", "sections", "sub_topics", "topics"):
                if isinstance(module.get(alt_key), list):
                    sub_modules = module[alt_key]
                    break
        if not isinstance(sub_modules, list):
            sub_modules = []

        processed_sub_modules: list[dict[str, Any]] = []
        for sub_module in sub_modules:
            if not isinstance(sub_module, dict):
                continue

            activities = sub_module.get("activities")
            if not isinstance(activities, list):
                for alt_key in ("tasks", "lessons", "items", "exercises"):
                    if isinstance(sub_module.get(alt_key), list):
                        activities = sub_module[alt_key]
                        break
            if activities is None:
                # If no nested activities are provided, treat the sub-module itself as one activity.
                activities = [
                    {
                        "title": sub_module.get("title")
                        or sub_module.get("name")
                        or "Practice",
                        "description": sub_module.get("description")
                        or sub_module.get("summary")
                        or "Learning activity",
                        "difficulty_level": sub_module.get("difficulty")
                        or sub_module.get("difficulty_level"),
                    }
                ]
            if not isinstance(activities, list):
                activities = []

            processed_activities: list[dict[str, Any]] = []
            for activity in activities:
                if not isinstance(activity, dict):
                    continue
                processed_activities.append(
                    {
                        "title": str(
                            activity.get("title")
                            or activity.get("name")
                            or activity.get("activity_title")
                            or activity.get("task")
                            or "Activity"
                        ),
                        "description": str(
                            activity.get("description")
                            or activity.get("summary")
                            or activity.get("details")
                            or activity.get("content")
                            or "Activity details"
                        ),
                        "learning_objectives": coerce_string_list(
                            activity.get("learning_objectives")
                            or activity.get("objectives")
                            or activity.get("outcomes")
                        ),
                        "duration": str(activity.get("duration") or "1 hour"),
                        "difficulty_level": normalize_path_difficulty(
                            activity.get("difficulty_level") or activity.get("difficulty")
                        ),
                        "prerequisites": coerce_string_list(activity.get("prerequisites")),
                        "assessment_criteria": coerce_string_list(
                            activity.get("assessment_criteria")
                            or activity.get("assessmentCriteria")
                            or activity.get("success_criteria")
                        ),
                        "type": activity.get("type"),
                    }
                )

            processed_sub_modules.append(
                {
                    "title": str(
                        sub_module.get("title")
                        or sub_module.get("name")
                        or sub_module.get("sub_module_title")
                        or "Sub-module"
                    ),
                    "description": str(
                        sub_module.get("description")
                        or sub_module.get("summary")
                        or sub_module.get("overview")
                        or "Sub-module details"
                    ),
                    "activities": processed_activities,
                    "estimated_duration": str(
                        sub_module.get("estimated_duration") or sub_module.get("duration") or "3 hours"
                    ),
                    "learning_outcomes": coerce_string_list(
                        sub_module.get("learning_outcomes")
                        or sub_module.get("outcomes")
                        or sub_module.get("objectives")
                    ),
                }
            )

        processed_modules.append(
            {
                "title": str(
                    module.get("title") or module.get("name") or module.get("module_title") or "Module"
                ),
                "description": str(
                    module.get("description")
                    or module.get("summary")
                    or module.get("overview")
                    or "Module details"
                ),
                "sub_modules": processed_sub_modules,
                "duration": str(module.get("duration") or module.get("estimated_duration") or "1 week"),
                "objectives": coerce_string_list(
                    module.get("objectives") or module.get("learning_objectives") or module.get("outcomes")
                ),
                "difficulty_level": normalize_path_difficulty(
                    module.get("difficulty_level") or module.get("difficulty")
                ),
                "prerequisites": coerce_string_list(module.get("prerequisites")),
            }
        )

    if not processed_modules and isinstance(payload.get("modules"), list):
        # Last-resort coercion: map string modules into simple structures.
        for raw_module in payload["modules"]:
            if isinstance(raw_module, str):
                processed_modules.append(
                    {
                        "title": raw_module,
                        "description": raw_module,
                        "sub_modules": [
                            {
                                "title": "Core topics",
                                "description": raw_module,
                                "activities": [
                                    {
                                        "title": "Study",
                                        "description": raw_module,
                                        "learning_objectives": [],
                                        "duration": "1 hour",
                                        "difficulty_level": "intermediate",
                                        "prerequisites": [],
                                        "assessment_criteria": [],
                                        "type": "text",
                                    }
                                ],
                                "estimated_duration": "3 hours",
                                "learning_outcomes": [],
                            }
                        ],
                        "duration": "1 week",
                        "objectives": [],
                        "difficulty_level": "intermediate",
                        "prerequisites": [],
                    }
                )

    return {
        **payload,
        "modules": processed_modules,
        "estimated_completion_time": str(
            payload.get("estimated_completion_time")
            or payload.get("estimatedCompletionTime")
            or payload.get("completion_time")
            or "4 weeks"
        ),
        "prerequisites": coerce_string_list(payload.get("prerequisites")),
        "user_pace": str(payload.get("user_pace") or payload.get("userPace") or "normal"),
        "quiz_adaptations": coerce_string_list(
            payload.get("quiz_adaptations") or payload.get("quizAdaptations")
        ),
    }


def normalize_content_module_payload(payload: dict[str, Any]) -> dict[str, Any]:
    """Normalize common model output variants to GeneratedContentModule schema."""
    if "content" not in payload:
        for alt_key in ("sections", "content_sections", "contentSections", "lessons", "items"):
            if isinstance(payload.get(alt_key), list):
                payload = {**payload, "content": payload[alt_key]}
                break

    content_sections = payload.get("content")
    if not isinstance(content_sections, list):
        content_sections = []

    processed_sections: list[dict[str, str]] = []
    for section in content_sections:
        if not isinstance(section, dict):
            continue

        section_title = str(
            section.get("title") or section.get("heading") or section.get("name") or "Section"
        )
        section_body = section.get("content") or section.get("body") or section.get("description") or section.get("text")

        if not section_body:
            parts: list[str] = []
            for key in (
                "explanation",
                "details",
                "example",
                "key_takeaways",
                "takeaways",
                "reflection_questions",
                "practice_questions",
            ):
                value = section.get(key)
                if isinstance(value, list):
                    parts.append("\n".join(str(v) for v in value if v is not None))
                elif value is not None:
                    parts.append(str(value))
            section_body = "\n\n".join(p for p in parts if p).strip() or "Content section."

        processed_sections.append({"title": section_title, "content": str(section_body)})

    return {
        **payload,
        "title": str(payload.get("title") or payload.get("module_title") or payload.get("name") or "Generated Content"),
        "content": processed_sections,
        "learning_objectives": coerce_string_list(
            payload.get("learning_objectives") or payload.get("objectives")
        ),
        "estimated_completion": str(
            payload.get("estimated_completion") or payload.get("estimatedCompletion") or payload.get("duration") or "1 week"
        ),
    }


def call_ollama_structured(prompt: str, schema: type[TModel]) -> TModel:
    """Call Ollama with JSON schema and validate with Pydantic."""
    model_name = get_ollama_model()
    try:
        client = get_ollama_client()
        response = client.chat(
            model=model_name,
            messages=[{"role": "user", "content": prompt}],
            format=schema.model_json_schema(),
            options={"temperature": 0.1, "num_ctx": OLLAMA_NUM_CTX},
        )
    except Exception as e:
        logger.error(f"Ollama request failed for model {model_name} on {OLLAMA_HOST}: {str(e)}")
        raise HTTPException(status_code=502, detail=f"Ollama request failed: {str(e)}")

    raw_content = response.message.content
    json_payload = extract_json_payload(raw_content)
    try:
        return schema.model_validate_json(json_payload)
    except ValidationError as e:
        if schema is CourseRecommendationsGeneration:
            try:
                raw_obj = json.loads(json_payload)
                coerced_obj = coerce_recommendations_payload(raw_obj)
                return schema.model_validate(coerced_obj)
            except (ValueError, ValidationError):
                pass
        if schema is GeneratedLearningPath:
            try:
                raw_obj = json.loads(json_payload)
                coerced_obj = normalize_learning_path_payload(raw_obj)
                return schema.model_validate(coerced_obj)
            except (ValueError, ValidationError):
                pass
        if schema is GeneratedContentModule:
            try:
                raw_obj = json.loads(json_payload)
                coerced_obj = normalize_content_module_payload(raw_obj)
                return schema.model_validate(coerced_obj)
            except (ValueError, ValidationError):
                pass

        logger.error(f"Structured output validation failed for {schema.__name__}: {str(e)}")
        logger.debug(f"Raw LLM output from {model_name}: {raw_content}")
        raise HTTPException(
            status_code=502,
            detail=f"LLM output failed {schema.__name__} schema validation",
        )


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initialize and validate backend resources at startup."""
    logger.info("Running startup initialization...")
    try:
        init_db()
        if verify_db_schema():
            logger.info("Database schema verified successfully")
        else:
            logger.error("Database schema verification failed")
            raise RuntimeError("Database initialization failed")
        get_ollama_client()
        selected_model = get_ollama_model()
        logger.info(f"Ollama backend ready on {OLLAMA_HOST} with model {selected_model}")
        app.state.http_client = httpx.AsyncClient(timeout=HTTP_TIMEOUT)
        yield
    except Exception as e:
        logger.error(f"Startup initialization failed: {str(e)}")
        raise RuntimeError(f"Startup initialization failed: {str(e)}")
    finally:
        client = getattr(app.state, "http_client", None)
        if client:
            await client.aclose()


# Initialize FastAPI app
app = FastAPI(
    title="AIPLP Learning Assessment Backend",
    version="2.0.0",
    lifespan=lifespan,
)

# Pydantic models for structured outputs
class CourseRecommendation(BaseModel):
    id: Optional[int] = None
    title: str
    description: str
    confidence_score: float

class CourseRecommendations(BaseModel):
    recommendations: list[CourseRecommendation]
    user_id: int


class CourseRecommendationsGeneration(BaseModel):
    recommendations: list[CourseRecommendation]


class ContentSection(BaseModel):
    title: str
    content: str


class GeneratedActivityComponent(BaseModel):
    title: str
    description: str
    learning_objectives: list[str] = Field(default_factory=list)
    duration: str = "1 hour"
    difficulty_level: Literal["beginner", "intermediate", "advanced"] = "intermediate"
    prerequisites: list[str] = Field(default_factory=list)
    assessment_criteria: list[str] = Field(default_factory=list)
    type: Optional[str] = None


class GeneratedSubModule(BaseModel):
    title: str
    description: str
    activities: list[GeneratedActivityComponent]
    estimated_duration: str = "3 hours"
    learning_outcomes: list[str] = Field(default_factory=list)


class GeneratedModuleActivity(BaseModel):
    title: str
    description: str
    sub_modules: list[GeneratedSubModule]
    duration: str = "1 week"
    objectives: list[str] = Field(default_factory=list)
    difficulty_level: Literal["beginner", "intermediate", "advanced"] = "intermediate"
    prerequisites: list[str] = Field(default_factory=list)


class GeneratedLearningPath(BaseModel):
    modules: list[GeneratedModuleActivity]
    estimated_completion_time: str = "4 weeks"
    prerequisites: list[str] = Field(default_factory=list)
    user_pace: str = "normal"
    quiz_adaptations: list[str] = Field(default_factory=list)


class ActivityComponent(BaseModel):
    id: str
    title: str
    description: str
    learning_objectives: list[str] = Field(default_factory=list)
    duration: str = "1 hour"
    difficulty_level: Literal["beginner", "intermediate", "advanced"] = "intermediate"
    prerequisites: list[str] = Field(default_factory=list)
    assessment_criteria: list[str] = Field(default_factory=list)
    type: Optional[str] = None

class SubModule(BaseModel):
    id: str
    title: str
    description: str
    activities: list[ActivityComponent]
    estimated_duration: str = "3 hours"
    learning_outcomes: list[str] = Field(default_factory=list)

class ModuleActivity(BaseModel):
    id: str
    title: str
    description: str
    sub_modules: list[SubModule]
    duration: str = "1 week"
    objectives: list[str] = Field(default_factory=list)
    difficulty_level: Literal["beginner", "intermediate", "advanced"] = "intermediate"
    prerequisites: list[str] = Field(default_factory=list)

class LearningPath(BaseModel):
    modules: list[ModuleActivity]
    estimated_completion_time: str = "4 weeks"
    prerequisites: list[str] = Field(default_factory=list)
    user_pace: str = "normal"
    quiz_adaptations: list[str] = Field(default_factory=list)

class ContentItem(BaseModel):
    id: str
    type: Literal["text", "video", "interactive", "exercise"]
    title: str
    content: str
    duration: str = "30 minutes"
    difficulty: Literal["basic", "intermediate", "advanced"] = "intermediate"
    learning_objectives: list[str] = Field(default_factory=list)
    quiz_related_focus: Optional[list[str]] = None
    parent_component_id: str


class GeneratedContentModule(BaseModel):
    title: str
    content: list[ContentSection]
    learning_objectives: list[str] = Field(default_factory=list)
    estimated_completion: str = "1 week"


class ContentModule(BaseModel):
    id: str
    title: str
    content: list[ContentItem]
    learning_objectives: list[str] = Field(default_factory=list)
    estimated_completion: str = "1 week"
    parent_module_id: str

# Pydantic models for request validation
class SurveyResponse(BaseModel):
    careerField: str
    learningMotivation: str
    preferredLearningFormat: str
    professionalStatus: str
    skillDevelopmentGoal: str
    timeAvailability: str
    learningChallenges: str
    onlineLearningExperience: str
    learningExperience: str
    techComfortLevel: str

class QuizAnswer(BaseModel):
    question: str
    selectedAnswer: str
    correct: bool
    questionNumber: int
    topic: str

class UserQuizData(BaseModel):
    userId: int
    answers: list[QuizAnswer]


def normalize_content_difficulty(level: str) -> Literal["basic", "intermediate", "advanced"]:
    """Normalize learning-path difficulty labels to content labels."""
    normalized = level.lower()
    if normalized == "beginner":
        return "basic"
    if normalized == "advanced":
        return "advanced"
    return "intermediate"



def init_db():
    """Initialize database with updated schema"""
    logger.info("Initializing database...")
    conn = None
    try:
        conn = get_db_connection()
        c = conn.cursor()
        
        # Create tables with updated schema
        c.execute('''
            CREATE TABLE IF NOT EXISTS user_data (
                user_id INTEGER PRIMARY KEY,
                survey_data TEXT,
                quiz_data TEXT,
                quiz_performance_summary TEXT,
                last_updated TIMESTAMP
            )
        ''')
        
        c.execute('''
            CREATE TABLE IF NOT EXISTS course_recommendations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                course_title TEXT,
                course_description TEXT,
                confidence_score FLOAT,
                timestamp TIMESTAMP,
                quiz_influenced_modifications TEXT,
                FOREIGN KEY (user_id) REFERENCES user_data (user_id)
            )
        ''')
        
        c.execute('''
            CREATE TABLE IF NOT EXISTS learning_paths (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                course_id INTEGER,
                path_content TEXT,
                quiz_adaptations TEXT,
                user_pace TEXT,
                timestamp TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES user_data (user_id),
                FOREIGN KEY (course_id) REFERENCES course_recommendations (id)
            )
        ''')
        
        c.execute('''
            CREATE TABLE IF NOT EXISTS course_content (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                course_id INTEGER,
                content TEXT,
                quiz_based_modifications TEXT,
                pace_adjustments TEXT,
                timestamp TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES user_data (user_id),
                FOREIGN KEY (course_id) REFERENCES course_recommendations (id)
            )
        ''')
        
        conn.commit()
        logger.info("Database initialization completed successfully")
    except Exception as e:
        logger.error(f"Database initialization failed: {str(e)}")
        raise
    finally:
        if conn:
            conn.close()

def verify_db_schema():
    """Verify database schema and report status"""
    conn = None
    try:
        conn = get_db_connection()
        c = conn.cursor()
        
        # Check for all required tables and columns
        c.execute("SELECT name FROM sqlite_master WHERE type='table'")
        tables = [table[0] for table in c.fetchall()]
        
        expected_tables = ['user_data', 'course_recommendations', 'learning_paths', 'course_content']
        missing_tables = [table for table in expected_tables if table not in tables]
        
        if missing_tables:
            logger.error(f"Missing tables: {missing_tables}")
            return False
            
        # Verify user_data columns
        c.execute("PRAGMA table_info(user_data)")
        columns = [column[1] for column in c.fetchall()]
        expected_columns = ['user_id', 'survey_data', 'quiz_data', 'quiz_performance_summary', 'last_updated']
        missing_columns = [col for col in expected_columns if col not in columns]
        
        if missing_columns:
            logger.error(f"Missing columns in user_data: {missing_columns}")
            return False
            
        return True
        
    except Exception as e:
        logger.error(f"Error verifying database schema: {str(e)}")
        return False
    finally:
        if conn:
            conn.close()

def categorize_question(question: str) -> str:
    """Categorize questions into topics based on their content"""
    question_lower = question.lower()
    
    categories = {
        'logical_reasoning': ['if all', 'then', 'reasoning', 'abstract'],
        'mathematical': ['number', 'produce', 'pattern', 'how many'],
        'learning_style': ['learn best', 'learning preference', 'information processing'],
        'problem_solving': ['problem-solving', 'approach', 'complex problem'],
        'study_habits': ['time management', 'approach', 'project'],
        'motivation': ['motivates', 'motivation']
    }

    for category, keywords in categories.items():
        if any(keyword in question_lower for keyword in keywords):
            return category
            
    return 'general'

def analyze_quiz_performance(quiz_data: dict) -> dict[str, Any]:
    """Analyze quiz performance to identify strengths and weaknesses"""
    try:
        # Make sure we're working with the answers list
        answers = quiz_data.get('answers', [])
        if not answers:
            return {
                'topic_scores': {},
                'overall_score': 0,
                'weak_areas': []
            }

        # Initialize topic performance tracking
        topic_performance = {}
        
        # Analyze each answer and categorize by derived topic
        for answer in answers:
            question = answer.get('question', '')
            topic = categorize_question(question)
            
            if topic not in topic_performance:
                topic_performance[topic] = {'correct': 0, 'total': 0}
            
            topic_performance[topic]['total'] += 1
            if answer.get('correct', False):
                topic_performance[topic]['correct'] += 1
        
        # Calculate topic scores and identify weak areas
        topic_scores = {}
        weak_areas = []
        
        for topic, data in topic_performance.items():
            if data['total'] > 0:
                score = (data['correct'] / data['total']) * 100
                topic_scores[topic] = round(score, 2)  # Round to 2 decimal places
                if score < 60:
                    weak_areas.append(topic)

        # Calculate overall score
        total_correct = sum(data['correct'] for data in topic_performance.values())
        total_questions = sum(data['total'] for data in topic_performance.values())
        overall_score = round((total_correct / total_questions * 100), 2) if total_questions > 0 else 0

        # Add analysis summary
        performance_summary = {
            'topic_scores': topic_scores,
            'overall_score': overall_score,
            'weak_areas': weak_areas,
            'strengths': [topic for topic, score in topic_scores.items() if score >= 80],
            'total_questions_answered': total_questions,
            'question_distribution': {
                topic: data['total'] for topic, data in topic_performance.items()
            }
        }

        return performance_summary
        
    except Exception as e:
        logger.error(f"Error in analyze_quiz_performance: {str(e)}")
        return {
            'topic_scores': {},
            'overall_score': 0,
            'weak_areas': [],
            'strengths': [],
            'total_questions_answered': 0,
            'question_distribution': {}
        }

async def fetch_user_data(
    user_id: int,
    http_client: httpx.AsyncClient,
) -> tuple[dict[str, Any], dict[str, Any], dict[str, Any]]:
    """Fetch and analyze user data from Spring Boot application"""
    try:
        survey_response = await http_client.get(f"{SPRING_BOOT_API_BASE}/api/data/survey-responses")
        survey_response.raise_for_status()
        survey_payload = survey_response.json()
        survey_data = next((sr for sr in survey_payload if sr.get("userId") == user_id), None)
        if not survey_data:
            raise HTTPException(status_code=404, detail=f"Survey data not found for user_id {user_id}")
        
        quiz_response = await http_client.get(f"{SPRING_BOOT_API_BASE}/api/data/quiz-answers")
        quiz_response.raise_for_status()
        quiz_payload = quiz_response.json()
        quiz_data = next((qd for qd in quiz_payload if qd.get("userId") == user_id), None)
        if not quiz_data:
            raise HTTPException(status_code=404, detail=f"Quiz data not found for user_id {user_id}")

        # Analyze quiz performance
        quiz_analysis = analyze_quiz_performance(quiz_data)

        with get_db_connection() as conn:
            c = conn.cursor()
            current_time = utc_now()
            c.execute('''
                INSERT OR REPLACE INTO user_data 
                (user_id, survey_data, quiz_data, quiz_performance_summary, last_updated)
                VALUES (?, ?, ?, ?, ?)
            ''', (
                user_id,
                json.dumps(survey_data),
                json.dumps(quiz_data),
                json.dumps(quiz_analysis),
                current_time,
            ))
            conn.commit()
        
        return survey_data, quiz_data, quiz_analysis
    except httpx.HTTPStatusError as e:
        logger.error(f"Spring Boot API returned error: {str(e)}")
        raise HTTPException(status_code=502, detail=f"Upstream API failure: {e.response.status_code}")
    except httpx.RequestError as e:
        logger.error(f"Unable to reach Spring Boot API: {str(e)}")
        raise HTTPException(status_code=503, detail="Failed to connect to upstream API")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error fetching user data: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

def generate_course_recommendations(
    survey_data: dict,
    quiz_data: dict,
    quiz_analysis: dict,
    user_id: int,
) -> dict[str, Any]:
    """Generate personalized course recommendations using Ollama with structured output"""
    try:
        prompt = f"""Generate course recommendations based on:
        Profile:
        - Career Field: {survey_data['careerField']}
        - Learning Motivation: {survey_data['learningMotivation']}
        - Professional Status: {survey_data['professionalStatus']}
        - Skill Goal: {survey_data['skillDevelopmentGoal']}
        
        Quiz Performance:
        - Overall Score: {quiz_analysis['overall_score']}%
        - Weak Areas: {', '.join(quiz_analysis['weak_areas'])}
        - Topic Scores: {json.dumps(quiz_analysis['topic_scores'], indent=2)}
        
        Learning Style: {survey_data['preferredLearningFormat']}
        
        Return only JSON matching the provided schema.
        """
        recommendations = call_ollama_structured(prompt, CourseRecommendationsGeneration)
        recommendations_with_user = CourseRecommendations(
            user_id=user_id,
            recommendations=recommendations.recommendations,
        )
        return recommendations_with_user.model_dump()
            
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error generating recommendations: {str(e)}")
        raise HTTPException(status_code=500, detail="Failed to generate recommendations")

def generate_component_id(prefix: str, index: int, parent_id: str = "") -> str:
    """Generate unique IDs for learning path components"""
    if parent_id:
        return f"{parent_id}-{prefix}{index}"
    return f"{prefix}{index}"

def generate_learning_path(course: dict, user_data: dict, quiz_data: dict, quiz_analysis: dict) -> dict[str, Any]:
    """Generate detailed learning path with hierarchical structure and unique IDs"""
    try:
        prompt = f"""Create a detailed hierarchical learning path for "{course['title']}" with:
        
        Time & Experience Context:
        - Time availability: {user_data['timeAvailability']}
        - Learning challenges: {user_data['learningChallenges']}
        - Experience level: {user_data['learningExperience']}
        
        Performance Context:
        - Overall Score: {quiz_analysis['overall_score']}%
        - Weak Areas: {', '.join(quiz_analysis['weak_areas'])}
        - Topic Scores: {json.dumps(quiz_analysis['topic_scores'], indent=2)}
        
        Requirements:
        1. Create a detailed structure with modules, sub-modules, and activities
        2. Each component should have clear learning objectives
        3. Include detailed descriptions for each component
        4. Adapt difficulty based on quiz performance
        5. Consider time constraints in duration estimates
        
        Return only JSON matching the provided schema.
        """
        path_data = call_ollama_structured(prompt, GeneratedLearningPath).model_dump()
        processed_modules = []
        
        for module_idx, module in enumerate(path_data['modules']):
            module_id = generate_component_id('M', module_idx + 1)
            processed_sub_modules = []
            
            for sub_idx, sub_module in enumerate(module['sub_modules']):
                sub_module_id = generate_component_id('S', sub_idx + 1, module_id)
                processed_activities = []
                
                for act_idx, activity in enumerate(sub_module['activities']):
                    activity_id = generate_component_id('A', act_idx + 1, sub_module_id)
                    processed_activities.append({
                        **activity,
                        'id': activity_id
                    })
                
                processed_sub_modules.append({
                    **sub_module,
                    'id': sub_module_id,
                    'activities': processed_activities
                })
            
            processed_modules.append({
                **module,
                'id': module_id,
                'sub_modules': processed_sub_modules
            })
        
        path_data['modules'] = processed_modules
        validated_path = LearningPath.model_validate(path_data)
        return validated_path.model_dump()
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error generating learning path: {str(e)}")
        raise HTTPException(status_code=500, detail="Failed to generate learning path")

def generate_course_content(
    component_id: str,
    course: dict,
    learning_path: dict,
    user_data: dict,
    quiz_analysis: dict
) -> dict[str, Any]:
    """Generate text-based content for a specific component of the learning path"""
    try:
        # Find the component in the learning path
        component = find_component_by_id(learning_path, component_id)
        if not component:
            raise HTTPException(status_code=404, detail="Component not found")
            
        # Safely get learning objectives with fallback to empty list
        learning_objectives = component.get('learning_objectives', [])
        
        # Safely get difficulty level with fallback to 'intermediate'
        difficulty_level = component.get('difficulty_level', 'intermediate')
        
        # Find any weak areas that match the component title
        component_title = component.get('title', '').lower()
        related_weak_areas = [
            wa for wa in quiz_analysis.get('weak_areas', [])
            if wa in component_title
        ]
            
        prompt = f"""Generate detailed text-based educational content for component "{component.get('title', 'Unnamed Component')}" considering:
        
        Component Context:
        - Type: {get_component_type(component_id)}
        - Learning Objectives: {json.dumps(learning_objectives)}
        - Difficulty Level: {difficulty_level}
        
        User Context:
        - Learning Experience: {user_data.get('learningExperience', 'intermediate')}
        - Time Availability: {user_data.get('timeAvailability', 'not specified')}
        
        Performance Context:
        - Related Weak Areas: {json.dumps(related_weak_areas)}
        
        Requirements:
        1. Create only text-based content divided into clear sections
        2. Each section should:
           - Have a clear title
           - Include detailed explanations
           - Provide examples where appropriate
           - End with key takeaways
        3. Content should align with learning objectives
        4. Language should match user's experience level
        5. Include practice questions or reflection points
        
        Return only JSON matching the provided schema.
        """
        generated_content = call_ollama_structured(prompt, GeneratedContentModule)
        content = generated_content.model_dump()
        processed_content = []
        
        # Ensure all content items are text type
        for idx, item in enumerate(content['content']):
            content_id = generate_component_id('C', idx + 1, component_id)
            processed_content.append({
                'id': content_id,
                'type': 'text',
                'title': item['title'],
                'content': item['content'],
                'duration': '15 minutes',  # Default duration for text content
                'difficulty': normalize_content_difficulty(difficulty_level),
                'learning_objectives': learning_objectives,
                'parent_component_id': component_id
            })
            
        content['content'] = processed_content
        content['id'] = f"CNT-{component_id}"
        content['parent_module_id'] = component_id
        content['learning_objectives'] = learning_objectives
        content['estimated_completion'] = f"{len(processed_content) * 15} minutes"

        validated_content = ContentModule.model_validate(content)
        return validated_content.model_dump()
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error generating content: {str(e)}")
        logger.error(f"Component ID: {component_id}")
        logger.error(f"Component data: {component if 'component' in locals() else 'Not found'}")
        raise HTTPException(
            status_code=500, 
            detail=f"Failed to generate content: {str(e)}"
        )

def find_component_by_id(learning_path: dict, component_id: str) -> Optional[dict]:
    """Find a component in the learning path by its ID"""
    for module in learning_path['modules']:
        if module['id'] == component_id:
            return module
            
        for sub_module in module.get('sub_modules', []):
            if sub_module['id'] == component_id:
                return sub_module
                
            for activity in sub_module.get('activities', []):
                if activity['id'] == component_id:
                    return activity
                    
    return None

def get_component_type(component_id: str) -> str:
    """Determine component type from ID"""
    if component_id.startswith('M'):
        return "module"
    elif component_id.startswith('S'):
        return "sub_module"
    elif component_id.startswith('A'):
        return "activity"
    return "unknown"

@app.get("/suggest_courses/{user_id}")
async def get_suggested_courses(user_id: int, request: Request):
    try:
        logger.info(f"Starting course suggestion for user_id: {user_id}")

        http_client = getattr(request.app.state, "http_client", None)
        if http_client is None:
            raise HTTPException(status_code=500, detail="HTTP client not initialized")

        survey_data, quiz_data, quiz_analysis = await fetch_user_data(user_id, http_client)
        recommendations = generate_course_recommendations(
            survey_data,
            quiz_data,
            quiz_analysis,
            user_id,
        )

        stored_recommendations: list[dict[str, Any]] = []
        with get_db_connection() as conn:
            c = conn.cursor()
            current_time = utc_now()
            for course in recommendations["recommendations"]:
                c.execute(
                    """
                    INSERT INTO course_recommendations 
                    (user_id, course_title, course_description, confidence_score,
                    quiz_influenced_modifications, timestamp)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    (
                        user_id,
                        course["title"],
                        course["description"],
                        course["confidence_score"],
                        json.dumps(quiz_analysis.get("weak_areas", [])),
                        current_time,
                    ),
                )
                course_with_id = {**course, "id": c.lastrowid}
                stored_recommendations.append(course_with_id)
            conn.commit()

        return {
            "recommendations": stored_recommendations,
            "user_id": user_id,
            "quiz_performance": quiz_analysis,
        }
    except HTTPException:
        raise
    except sqlite3.Error as e:
        logger.exception("Database failure in get_suggested_courses")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    except Exception as e:
        logger.exception("Unexpected error in get_suggested_courses")
        raise HTTPException(status_code=500, detail=f"An unexpected error occurred: {str(e)}")


@app.get("/learning_path/{user_id}/{course_id}")
async def get_learning_path(user_id: int, course_id: int):
    try:
        with get_db_connection() as conn:
            c = conn.cursor()

            c.execute(
                """
                SELECT survey_data, quiz_data, quiz_performance_summary
                FROM user_data
                WHERE user_id = ?
                """,
                (user_id,),
            )
            user_row = c.fetchone()
            if not user_row:
                raise HTTPException(status_code=404, detail="User not found")

            c.execute(
                """
                SELECT id, course_title, course_description, confidence_score
                FROM course_recommendations
                WHERE id = ? AND user_id = ?
                """,
                (course_id, user_id),
            )
            course_row = c.fetchone()
            if not course_row:
                raise HTTPException(status_code=404, detail="Course not found")

            user_data = json.loads(user_row[0])
            quiz_data = json.loads(user_row[1])
            quiz_analysis = json.loads(user_row[2])

            course = {
                "id": course_row[0],
                "title": course_row[1],
                "description": course_row[2],
                "confidence_score": course_row[3],
            }

            learning_path = generate_learning_path(
                course,
                user_data,
                quiz_data,
                quiz_analysis,
            )

            c.execute(
                """
                INSERT INTO learning_paths
                (user_id, course_id, path_content, quiz_adaptations, user_pace, timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                (
                    user_id,
                    course_id,
                    json.dumps(learning_path),
                    json.dumps(quiz_analysis["weak_areas"]),
                    learning_path.get("user_pace", "normal"),
                    utc_now(),
                ),
            )
            conn.commit()

        return {
            "learning_path": learning_path,
            "course_id": course_id,
            "user_id": user_id,
            "quiz_performance": quiz_analysis,
        }
    except HTTPException:
        raise
    except sqlite3.Error as e:
        logger.exception("Database failure in get_learning_path")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    except Exception as e:
        logger.exception("Unexpected error in get_learning_path")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/course_content/{user_id}/{course_id}/{component_id}")
async def get_component_content(user_id: int, course_id: int, component_id: str):
    try:
        with get_db_connection() as conn:
            c = conn.cursor()

            c.execute(
                """
                SELECT survey_data, quiz_data, quiz_performance_summary
                FROM user_data
                WHERE user_id = ?
                """,
                (user_id,),
            )
            user_row = c.fetchone()
            if not user_row:
                raise HTTPException(status_code=404, detail="User not found")

            c.execute(
                """
                SELECT id, course_title, course_description
                FROM course_recommendations
                WHERE id = ? AND user_id = ?
                """,
                (course_id, user_id),
            )
            course_row = c.fetchone()
            if not course_row:
                raise HTTPException(status_code=404, detail="Course not found")

            c.execute(
                """
                SELECT path_content
                FROM learning_paths
                WHERE course_id = ? AND user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
                (course_id, user_id),
            )
            path_row = c.fetchone()
            if not path_row:
                raise HTTPException(status_code=404, detail="Learning path not found")

            user_data = json.loads(user_row[0])
            quiz_analysis = json.loads(user_row[2])
            learning_path = json.loads(path_row[0])

            course = {
                "id": course_row[0],
                "title": course_row[1],
                "description": course_row[2],
            }

            content = generate_course_content(
                component_id,
                course,
                learning_path,
                user_data,
                quiz_analysis,
            )

            c.execute(
                """
                INSERT INTO course_content
                (user_id, course_id, content, quiz_based_modifications, pace_adjustments, timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                (
                    user_id,
                    course_id,
                    json.dumps(content),
                    json.dumps(quiz_analysis["weak_areas"]),
                    learning_path.get("user_pace", "normal"),
                    utc_now(),
                ),
            )
            conn.commit()

        return {
            "component_content": content,
            "component_id": component_id,
            "course_id": course_id,
            "user_id": user_id,
            "performance_context": {
                "quiz_performance": quiz_analysis,
                "component_type": get_component_type(component_id),
            },
        }
    except HTTPException:
        raise
    except sqlite3.Error as e:
        logger.exception("Database failure in get_component_content")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    except Exception as e:
        logger.exception("Unexpected error in get_component_content")
        raise HTTPException(status_code=500, detail=str(e))

# Add health check endpoint
@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "timestamp": utc_now().isoformat(),
        "database": "connected" if check_database_connection() else "disconnected"
    }

def check_database_connection() -> bool:
    try:
        with get_db_connection() as conn:
            conn.execute("SELECT 1")
        return True
    except Exception:
        return False

# Error handling middleware
@app.middleware("http")
async def add_error_handling(request: Request, call_next):
    try:
        response = await call_next(request)
        return response
    except HTTPException:
        raise
    except Exception as e:
        logger.exception(f"Unhandled error for {request.method} {request.url.path}: {str(e)}")
        return JSONResponse(
            status_code=500,
            content={
                "detail": "An unexpected error occurred",
                "timestamp": utc_now().isoformat()
            }
        )

if __name__ == "__main__":
    import uvicorn
    
    logger.info("Starting FastAPI server...")
    uvicorn.run(
        app, 
        host="0.0.0.0", 
        port=8000,
        log_level="debug",
        reload=True
    )
    
