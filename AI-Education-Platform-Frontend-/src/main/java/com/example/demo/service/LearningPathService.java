package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LearningPathService {
    
    public Map<String, Object> getLearningPath() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("static/fetched_learning_path.json");
            return mapper.readValue(resource.getInputStream(), Map.class);
        } catch (IOException e) {
            throw new RuntimeException("Error loading learning path", e);
        }
    }

    public List<Map<String, Object>> getModules() {
        Map<String, Object> learningPath = getLearningPath();
        Map<String, Object> pathData = (Map<String, Object>) learningPath.get("learning_path");
        return (List<Map<String, Object>>) pathData.get("modules");
    }

    public List<Map<String, Object>> getCourses() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("static/fetched_courses.json");
            Map<String, Object> coursesData = mapper.readValue(resource.getInputStream(), Map.class);
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) coursesData.get("recommendations");
            
            // Convert recommendations to course format
            return recommendations.stream()
                .map(rec -> {
                    Map<String, Object> course = new HashMap<>();
                    course.put("name", rec.get("title"));
                    course.put("description", rec.get("description"));
                    course.put("difficulty_level", "Intermediate"); // Default value
                    course.put("duration", "8 weeks"); // Default value
                    course.put("objectives", Arrays.asList(
                        "Master core concepts",
                        "Develop practical skills",
                        "Apply knowledge to real scenarios",
                        "Complete course projects"
                    ));
                    return course;
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error loading courses", e);
        }
    }
}
