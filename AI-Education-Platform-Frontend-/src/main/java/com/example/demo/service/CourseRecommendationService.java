package com.example.demo.service;

import com.example.demo.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CourseRecommendationService {
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private static final Logger logger = LoggerFactory.getLogger(CourseRecommendationService.class);
    
    @Autowired
    public CourseRecommendationService(ResourceLoader resourceLoader) {
        this.objectMapper = new ObjectMapper();
        this.resourceLoader = resourceLoader;
    }
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecommendedCourses(Long userId) {
        try {
            // Try to load from static/data/data first
            String resourcePath = String.format("classpath:static/data/data/suggest_courses_%d.json", userId);
            Resource resource = resourceLoader.getResource(resourcePath);
            
            if (!resource.exists()) {
                // Try alternate path without 'static' prefix
                resourcePath = String.format("classpath:data/data/suggest_courses_%d.json", userId);
                resource = resourceLoader.getResource(resourcePath);
                
                if (!resource.exists()) {
                    logger.warn("Course suggestions file not found for user {} at paths: classpath:static/data/data/ and classpath:data/data/", userId);
                    return new ArrayList<>();
                }
            }
            
            // Read the JSON array of user recommendations
            Map<String, Object> userData = objectMapper.readValue(resource.getInputStream(), Map.class);
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) userData.get("recommendations");
            
            if (recommendations == null || recommendations.isEmpty()) {
                logger.warn("No recommendations found in suggestions file for user {}", userId);
                return new ArrayList<>();
            }
            
            logger.info("Found {} recommendations for user {}", recommendations.size(), userId);
            return recommendations;
            
        } catch (Exception e) {
            logger.error("Error reading course suggestions file: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
