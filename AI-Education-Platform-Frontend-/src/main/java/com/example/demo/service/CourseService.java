package com.example.demo.service;

import com.example.demo.model.Course;
import com.example.demo.model.CourseProgress;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseService {
    private final ObjectMapper objectMapper;

    public CourseService() {
        this.objectMapper = new ObjectMapper();
    }

    @SuppressWarnings("unchecked")
    public Optional<String> getCourseTitle(String courseId) {
        try {
            // First, try reading from suggest_courses files
            for (int i = 1; i <= 50; i++) {  
                try {
                    String filePath = String.format("static/data/data/suggest_courses_%d.json", i);
                    
                    ClassPathResource resource = new ClassPathResource(filePath);
                    
                    if (!resource.exists()) {
                        continue;
                    }
                    
                    InputStream inputStream = resource.getInputStream();
                    Map<String, Object> courseData = objectMapper.readValue(inputStream, Map.class);
                    
                    // Get recommendations array
                    List<Map<String, Object>> recommendations = (List<Map<String, Object>>) courseData.get("recommendations");
                    if (recommendations != null) {
                        // Find the course by ID
                        for (Map<String, Object> course : recommendations) {
                            Object idObj = course.get("id");
                            
                            if (idObj != null) {
                                // Handle different number types
                                int storedId;
                                if (idObj instanceof Number) {
                                    storedId = ((Number) idObj).intValue();
                                } else {
                                    storedId = Integer.parseInt(idObj.toString());
                                }
                                
                                int searchId = Integer.parseInt(courseId);

                                if (storedId == searchId) {
                                    String title = (String) course.get("title");
                                    
                                    if (title != null && !title.trim().isEmpty()) {
                                        return Optional.of(title);
                                    }
                                }
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    // Stop searching if file is not found
                    break;
                } catch (Exception e) {
                    // Silently handle any reading errors and continue
                    continue;
                }
            }

            // Fallback: Generate a generic title
            return Optional.of(String.format("Course %s", courseId));
            
        } catch (Exception e) {
            // Fallback in case of any unexpected errors
            return Optional.of(String.format("Course %s", courseId));
        }
    }

    @SuppressWarnings("unchecked")
    public int getNotStartedCoursesCount(Long userId, List<CourseProgress> progressList) {
        try {
            // Read from the classpath resource
            String filePath = String.format("static/data/data/suggest_courses_%d.json", userId);
            InputStream inputStream = new ClassPathResource(filePath).getInputStream();
            Map<String, Object> courseData = objectMapper.readValue(inputStream, Map.class);
            
            // Get recommendations array
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) courseData.get("recommendations");
            if (recommendations == null) {
                return 0;
            }

            // Create a set of course IDs that have progress data
            Set<String> coursesWithProgress = progressList.stream()
                .map(CourseProgress::getCourseId)
                .collect(Collectors.toSet());

            // Count courses that are in recommendations but not in progress
            return (int) recommendations.stream()
                .map(course -> String.valueOf(((Number) course.get("id")).intValue()))
                .filter(courseId -> !coursesWithProgress.contains(courseId))
                .count();
            
        } catch (Exception e) {
            return 0;
        }
    }
}
