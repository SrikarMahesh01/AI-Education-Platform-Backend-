package com.example.demo.service;

import com.example.demo.model.QuizResult;
import com.example.demo.model.SurveyResponse;
import com.example.demo.model.QuizAnswer;
import com.example.demo.dto.QuizAnswerDTO;
import com.example.demo.dto.UserQuizAnswersDTO;
import com.example.demo.dto.QuizAnswerExportDTO;
import com.example.demo.repository.QuizResultRepository;
import com.example.demo.repository.SurveyRepository;
import com.example.demo.repository.QuizAnswerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedHashMap;

@Service
public class DataExportService {
    private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);
    private static final String QUIZ_RESULTS_FILE = "quiz_results.json";
    private static final String SURVEY_RESULTS_FILE = "survey_results.json";
    private static final String QUIZ_ANSWERS_FILE = "quiz_answers.json";

    @Autowired
    private QuizResultRepository quizResultRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    private final ObjectMapper objectMapper;

    public DataExportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void exportDataOnStartup() {
        logger.info("Exporting data on application startup");
        exportAllData();
    }

    public void exportAllData() {
        exportQuizResults();
        exportSurveyResponses();
        exportQuizAnswers();
    }

    public void exportQuizResults() {
        try {
            List<QuizResult> quizResults = quizResultRepository.findAll();
            if (quizResults.isEmpty()) {
                logger.info("No quiz results found to export");
                return;
            }
            // Transform to include user ID
            List<Map<String, Object>> exportResults = quizResults.stream()
                .map(result -> {
                    Map<String, Object> exportResult = new HashMap<>();
                    exportResult.put("id", result.getId());
                    exportResult.put("userId", result.getUser().getId());
                    exportResult.put("score", result.getScore());
                    exportResult.put("totalQuestions", result.getTotalQuestions());
                    exportResult.put("percentage", result.getPercentage());
                    exportResult.put("submissionTime", result.getSubmissionTime());
                    return exportResult;
                })
                .collect(Collectors.toList());
            objectMapper.writeValue(new File(QUIZ_RESULTS_FILE), exportResults);
            logger.info("Successfully exported {} quiz results to {}", quizResults.size(), QUIZ_RESULTS_FILE);
        } catch (IOException e) {
            logger.error("Error exporting quiz results: {}", e.getMessage());
        }
    }

    public void exportSurveyResponses() {
        try {
            List<SurveyResponse> surveyResponses = surveyRepository.findAll();
            if (surveyResponses.isEmpty()) {
                logger.info("No survey responses found to export");
                return;
            }
            // Transform to match exact JSON format
            List<Map<String, Object>> exportResponses = surveyResponses.stream()
                .map(response -> {
                    Map<String, Object> exportResponse = new HashMap<>();
                    exportResponse.put("id", response.getId());
                    exportResponse.put("userId", response.getUser().getId());
                    exportResponse.put("professionalStatus", response.getProfessionalStatus());
                    exportResponse.put("learningMotivation", response.getLearningMotivation());
                    exportResponse.put("timeAvailability", response.getTimeAvailability());
                    exportResponse.put("learningExperience", response.getLearningExperience());
                    
                    // Add additional fields to match the JSON format
                    exportResponse.put("careerField", response.getCareerField());
                    exportResponse.put("preferredLearningFormat", response.getPreferredLearningFormat());
                    exportResponse.put("skillDevelopmentGoal", response.getSkillDevelopmentGoal());
                    exportResponse.put("learningChallenges", response.getLearningChallenges());
                    exportResponse.put("onlineLearningExperience", response.getOnlineLearningExperience());
                    exportResponse.put("techComfortLevel", response.getTechComfortLevel());
                    
                    return exportResponse;
                })
                .collect(Collectors.toList());
            
            objectMapper.writeValue(new File(SURVEY_RESULTS_FILE), exportResponses);
            logger.info("Successfully exported {} survey responses to {}", surveyResponses.size(), SURVEY_RESULTS_FILE);
        } catch (IOException e) {
            logger.error("Error exporting survey responses: {}", e.getMessage());
        }
    }

    public void exportQuizAnswers() {
        try {
            List<QuizAnswer> answers = quizAnswerRepository.findAll();
            if (answers.isEmpty()) {
                logger.info("No quiz answers found to export");
                return;
            }
            
            // Group answers by user and sort by question number
            Map<Long, List<Map<String, Object>>> answersByUser = answers.stream()
                .collect(Collectors.groupingBy(
                    answer -> answer.getUser().getId(),
                    Collectors.mapping(
                        answer -> {
                            Map<String, Object> answerMap = new HashMap<>();
                            answerMap.put("question", answer.getQuestion());
                            answerMap.put("selectedAnswer", answer.getSelectedAnswer());
                            answerMap.put("questionNumber", answer.getQuestionNumber());
                            answerMap.put("correct", answer.isCorrect());
                            return answerMap;
                        },
                        Collectors.toList()
                    )
                ));
            
            // Sort the answers within each user's list by questionNumber
            answersByUser.values().forEach(userAnswers -> 
                userAnswers.sort(Comparator.comparingInt(a -> (Integer) a.get("questionNumber")))
            );
            
            // Create the final formatted list
            List<Map<String, Object>> formattedAnswers = answersByUser.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> userAnswers = new LinkedHashMap<>();
                    userAnswers.put("userId", entry.getKey());
                    userAnswers.put("answers", entry.getValue());
                    return userAnswers;
                })
                .collect(Collectors.toList());
            
            objectMapper.writeValue(new File(QUIZ_ANSWERS_FILE), formattedAnswers);
            logger.info("Successfully exported {} quiz answers to {}", answers.size(), QUIZ_ANSWERS_FILE);
        } catch (IOException e) {
            logger.error("Error exporting quiz answers: {}", e.getMessage());
        }
    }
}
