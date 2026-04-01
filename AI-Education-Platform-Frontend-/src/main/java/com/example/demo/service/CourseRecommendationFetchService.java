package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseRecommendationFetchService {

    private static final Logger logger = LoggerFactory.getLogger(CourseRecommendationFetchService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String LOG_FILE_NAME = "suggest.log";
    private static final String SUGGESTIONS_DIR = "static/data/data/";

    @Value("${app.log.directory:#{systemProperties['user.dir']}}")
    private String logDirectory;

    @Autowired
    public CourseRecommendationFetchService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private void logToFile(String message) {
        try {
            // Use provided log directory or default to current working directory
            File logFile = new File(logDirectory, LOG_FILE_NAME);
            
            // Ensure log directory exists
            logFile.getParentFile().mkdirs();

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                writer.println(String.format("[%s] %s", timestamp, message));
            }
        } catch (IOException e) {
            // Fallback logging
            logger.error("Failed to write to log file", e);
        }
    }

    public boolean fetchAndSaveCourseRecommendations(Long userId) {
        logToFile(String.format("Starting course recommendation fetch for user %d", userId));

        try {
            // Explicitly use the source resources directory
            String basePath = "src/main/resources/static/data/data/";
            File baseDir = new File(basePath);
            
            // Ensure the directory exists
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            File saveFile = new File(baseDir, "suggest_courses_" + userId + ".json");

            // Check if recommendations file already exists
            if (saveFile.exists()) {
                long fileAge = System.currentTimeMillis() - saveFile.lastModified();
                // If file is less than 24 hours old, use existing file
                if (fileAge < 24 * 60 * 60 * 1000) {
                    logToFile(String.format("Existing recommendations file found for user %d. Skipping fetch.", userId));
                    return true;
                }
            }

            // Log the start of API call
            logToFile(String.format("Attempting to fetch recommendations from http://localhost:8000/suggest_courses/%d", userId));
            
            // Fetch recommendations from the external API
            String url = String.format("http://localhost:8000/suggest_courses/%d", userId);
            Map<String, Object> recommendationResponse = restTemplate.getForObject(url, Map.class);

            if (recommendationResponse == null) {
                logToFile(String.format("No course recommendations found for user %d", userId));
                logger.warn("No course recommendations found for user {}", userId);
                return false;
            }

            // Extract recommendations
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) recommendationResponse.get("recommendations");
            
            if (recommendations == null || recommendations.isEmpty()) {
                logToFile(String.format("No course recommendations found for user %d", userId));
                logger.warn("No course recommendations found for user {}", userId);
                return false;
            }

            // Log course IDs
            List<Integer> courseIds = recommendations.stream()
                .map(rec -> (Integer) rec.get("id"))
                .collect(Collectors.toList());
            
            logToFile(String.format("Fetched %d course recommendations for user %d. Course IDs: %s", 
                recommendations.size(), userId, courseIds));

            // Write recommendations to file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(saveFile, recommendationResponse);

            // Log successful save
            logToFile(String.format("Successfully saved course recommendations for user %d to %s", userId, saveFile.getAbsolutePath()));
            logger.info("Successfully saved course recommendations for user {} to {}", userId, saveFile.getAbsolutePath());
            
            return true;

        } catch (Exception e) {
            // Log detailed error information
            String errorMessage = String.format("Error fetching or saving course recommendations for user %d: %s", 
                                                userId, e.getMessage());
            logToFile(errorMessage);
            logger.error(errorMessage, e);
            return false;
        } finally {
            logToFile(String.format("Completed recommendation fetch process for user %d", userId));
        }
    }
}
