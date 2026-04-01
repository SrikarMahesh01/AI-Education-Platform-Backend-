package com.example.demo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.repository.QuizResultRepository;
import com.example.demo.repository.SurveyRepository;
import com.example.demo.repository.QuizAnswerRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.model.QuizResult;
import com.example.demo.model.SurveyResponse;
import com.example.demo.model.QuizAnswer;
import com.example.demo.model.User;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Comparator;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/data")
public class DataApiController {

    @Autowired
    private QuizResultRepository quizResultRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user")
    public ResponseEntity<?> getUsers() {
        try {
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> exportUsers = users.stream()
                .map(user -> {
                    Map<String, Object> exportUser = new HashMap<>();
                    exportUser.put("id", user.getId());
                    exportUser.put("firstName", user.getFirstName());
                    exportUser.put("lastName", user.getLastName());
                    exportUser.put("email", user.getEmail());
                    exportUser.put("quizCompleted", user.getQuizCompleted());
                    exportUser.put("surveyCompleted", user.getSurveyCompleted());
                    // Exclude sensitive information like password
                    return exportUser;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(exportUsers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching users: " + e.getMessage());
        }
    }

    @GetMapping("/quiz_results")
    public ResponseEntity<?> getQuizResults() {
        try {
            List<QuizResult> results = quizResultRepository.findAll();
            List<Map<String, Object>> exportResults = results.stream()
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
            return ResponseEntity.ok(exportResults);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching quiz results: " + e.getMessage());
        }
    }

    @GetMapping("/survey_responses")
    public ResponseEntity<?> getSurveyResponses() {
        try {
            List<SurveyResponse> responses = surveyRepository.findAll();
            List<Map<String, Object>> exportResponses = responses.stream()
                .map(response -> {
                    Map<String, Object> exportResponse = new HashMap<>();
                    exportResponse.put("id", response.getId());
                    exportResponse.put("userId", response.getUser().getId());
                    exportResponse.put("professionalStatus", response.getProfessionalStatus());
                    exportResponse.put("learningMotivation", response.getLearningMotivation());
                    exportResponse.put("timeAvailability", response.getTimeAvailability());
                    exportResponse.put("learningExperience", response.getLearningExperience());
                    exportResponse.put("preferredLearningFormat", response.getPreferredLearningFormat());
                    exportResponse.put("techComfortLevel", response.getTechComfortLevel());
                    exportResponse.put("careerField", response.getCareerField());
                    exportResponse.put("learningChallenges", response.getLearningChallenges());
                    exportResponse.put("onlineLearningExperience", response.getOnlineLearningExperience());
                    exportResponse.put("skillDevelopmentGoal", response.getSkillDevelopmentGoal());
                    return exportResponse;
                })
                .collect(Collectors.toList());
            return ResponseEntity.ok(exportResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching survey responses: " + e.getMessage());
        }
    }

    @GetMapping("/quiz_answers")
    public ResponseEntity<?> getQuizAnswers() {
        try {
            List<QuizAnswer> answers = quizAnswerRepository.findAll();
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
            
            List<Map<String, Object>> formattedAnswers = answersByUser.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> userAnswers = new HashMap<>();
                    userAnswers.put("userId", entry.getKey());
                    userAnswers.put("answers", entry.getValue());
                    return userAnswers;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(formattedAnswers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching quiz answers: " + e.getMessage());
        }
    }

    @GetMapping("/quiz-answers")
    public ResponseEntity<?> getAllQuizAnswers() {
        try {
            List<QuizAnswer> answers = quizAnswerRepository.findAll();
            
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
            
            return ResponseEntity.ok(formattedAnswers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching quiz answers: " + e.getMessage());
        }
    }

    @GetMapping("/quiz-results")
    public ResponseEntity<List<QuizResult>> getAllQuizResults() {
        return ResponseEntity.ok(quizResultRepository.findAll());
    }

    @GetMapping("/survey-responses")
    public ResponseEntity<?> getAllSurveyResponses() {
        try {
            List<SurveyResponse> surveyResponses = surveyRepository.findAll();
            List<Map<String, Object>> formattedResponses = surveyResponses.stream()
                .map(response -> {
                    Map<String, Object> formattedResponse = new HashMap<>();
                    formattedResponse.put("id", response.getId());
                    formattedResponse.put("userId", response.getUser().getId());
                    formattedResponse.put("professionalStatus", response.getProfessionalStatus());
                    formattedResponse.put("learningMotivation", response.getLearningMotivation());
                    formattedResponse.put("timeAvailability", response.getTimeAvailability());
                    formattedResponse.put("learningExperience", response.getLearningExperience());
                    
                    // Add additional fields to match the JSON format
                    formattedResponse.put("careerField", response.getCareerField());
                    formattedResponse.put("preferredLearningFormat", response.getPreferredLearningFormat());
                    formattedResponse.put("skillDevelopmentGoal", response.getSkillDevelopmentGoal());
                    formattedResponse.put("learningChallenges", response.getLearningChallenges());
                    formattedResponse.put("onlineLearningExperience", response.getOnlineLearningExperience());
                    formattedResponse.put("techComfortLevel", response.getTechComfortLevel());
                    
                    return formattedResponse;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(formattedResponses);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching survey responses: " + e.getMessage());
        }
    }
}
