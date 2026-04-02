# AI-Education-Platform-Backend-

## Ollama Cloud configuration

This backend is configured to use **Ollama Cloud by default**.

Set the following environment variables before starting the server:

```bash
export OLLAMA_HOST="https://ollama.com"
export OLLAMA_API_KEY="your_ollama_api_key"
```

Optional:

```bash
export OLLAMA_MODEL="gpt-oss:20b"
export OLLAMA_NUM_CTX="4096"
```

Notes:

- If `OLLAMA_MODEL` is not set, the server auto-selects the best available cloud model from `/api/tags`.
- Structured JSON outputs are enforced with Pydantic schema validation for all model-generated responses.
