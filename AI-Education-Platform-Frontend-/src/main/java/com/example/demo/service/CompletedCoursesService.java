package com.example.demo.service;

import com.example.demo.model.CourseProgress;
import com.example.demo.repository.CourseProgressRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompletedCoursesService {
    private static final Logger logger = LoggerFactory.getLogger(CompletedCoursesService.class);

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private CourseService courseService;

    public void generateCompletedCoursesJson(Long userId) {
        try {
            logger.info("Starting to generate completed courses JSON for user {}", userId);
            
            // Get all completed courses for the user
            List<CourseProgress> completedCourses = courseProgressRepository.findByUserId(userId)
                .stream()
                .filter(progress -> progress.getProgressPercentage() == 100)
                .collect(Collectors.toList());

            logger.info("Found {} completed courses for user {}", completedCourses.size(), userId);

            ObjectMapper objectMapper = new ObjectMapper();
            ArrayNode coursesArray = objectMapper.createArrayNode();

            // Create JSON objects for each completed course
            for (CourseProgress progress : completedCourses) {
                try {
                    ObjectNode courseNode = objectMapper.createObjectNode();
                    courseNode.put("courseId", progress.getCourseId());
                    
                    // Detailed logging for course title retrieval
                    logger.debug("Attempting to get title for course ID: {}", progress.getCourseId());
                    
                    // Attempt to get course title, with detailed fallback
                    String courseTitle = courseService.getCourseTitle(progress.getCourseId())
                        .orElseGet(() -> {
                            logger.warn("Fallback to generic title for course ID: {}", progress.getCourseId());
                            return "Course " + progress.getCourseId();
                        });
                    
                    logger.info("Resolved course title: {} for ID: {}", courseTitle, progress.getCourseId());
                    
                    courseNode.put("courseTitle", courseTitle);
                    courseNode.put("completionDate", progress.getLastUpdated() != null 
                        ? progress.getLastUpdated().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) 
                        : "Unknown Date");
                    
                    coursesArray.add(courseNode);
                    logger.info("Added completed course: {} - {}", progress.getCourseId(), courseTitle);
                } catch (Exception e) {
                    logger.error("Error processing course {}: {}", progress.getCourseId(), e.getMessage(), e);
                }
            }

            // Get the absolute path to the project root
            File projectRoot = new File(System.getProperty("user.dir"));
            File dataDir = new File(projectRoot, "src/main/resources/static/data/data");
            dataDir.mkdirs();
            
            File outputFile = new File(dataDir, "completed_" + userId + ".json");
            logger.info("Writing completed courses to file: {}", outputFile.getAbsolutePath());
            
            // Write the JSON file
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, coursesArray);
            logger.info("Successfully generated completed courses JSON file at: {}", outputFile.getAbsolutePath());

        } catch (Exception e) {
            logger.error("Error generating completed courses JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating completed courses JSON", e);
        }
    }
}
