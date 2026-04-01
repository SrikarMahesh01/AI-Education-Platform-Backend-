package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.model.CourseProgress;
import com.example.demo.repository.CourseProgressRepository;
import com.example.demo.dto.CourseRecommendation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

@Service
public class RecommendationService {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    private static final int MAX_COURSES = 3;

    public boolean isCompletedCourse(User user, String courseId) {
        if (courseId == null) return false;
        Optional<CourseProgress> progress = courseProgressRepository.findByUserIdAndCourseId(user.getId(), courseId);
        return progress.isPresent() && progress.get().getProgressPercentage() >= 100;
    }

    public List<CourseRecommendation> getRecommendationsForUser(User user) {
        try {
            // Read the recommendations from JSON file
            ObjectMapper mapper = new ObjectMapper();
            ClassPathResource resource = new ClassPathResource("static/data/data/suggest_courses_" + user.getId() + ".json");
            JsonNode rootNode = mapper.readTree(resource.getInputStream());
            JsonNode recommendationsNode = rootNode.get("recommendations");

            // Get user's current courses as integers
            Set<Long> userCourses = new HashSet<>();
            if (user.getCourseSelected1() != null) userCourses.add(Long.parseLong(user.getCourseSelected1()));
            if (user.getCourseSelected2() != null) userCourses.add(Long.parseLong(user.getCourseSelected2()));
            if (user.getCourseSelected3() != null) userCourses.add(Long.parseLong(user.getCourseSelected3()));

            // Convert JSON to CourseRecommendation objects, excluding user's current courses
            List<CourseRecommendation> recommendations = new ArrayList<>();
            for (JsonNode courseNode : recommendationsNode) {
                Long courseId = courseNode.get("id").asLong();
                if (!userCourses.contains(courseId)) {
                    recommendations.add(new CourseRecommendation(
                        courseId,
                        courseNode.get("title").asText(),
                        courseNode.get("description").asText(),
                        courseNode.get("confidence_score").asDouble()
                    ));
                }
            }

            return recommendations;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Transactional
    public boolean assignCourseToUser(User user, Long courseId, Boolean replaceCompleted) {
        String courseIdStr = courseId.toString();
        
        // If replacing completed course
        if (Boolean.TRUE.equals(replaceCompleted)) {
            // Check each slot for a completed course
            if (isCompletedCourse(user, user.getCourseSelected1())) {
                user.setCourseSelected1(courseIdStr);
                user.setCourse1Progress(0);
                userService.updateUser(user);
                return true;
            }
            if (isCompletedCourse(user, user.getCourseSelected2())) {
                user.setCourseSelected2(courseIdStr);
                user.setCourse2Progress(0);
                userService.updateUser(user);
                return true;
            }
            if (isCompletedCourse(user, user.getCourseSelected3())) {
                user.setCourseSelected3(courseIdStr);
                user.setCourse3Progress(0);
                userService.updateUser(user);
                return true;
            }
            return false;
        }
        
        // If not replacing, find first empty slot
        if (user.getCourseSelected1() == null) {
            user.setCourseSelected1(courseIdStr);
            user.setCourse1Progress(0);
        } else if (user.getCourseSelected2() == null) {
            user.setCourseSelected2(courseIdStr);
            user.setCourse2Progress(0);
        } else if (user.getCourseSelected3() == null) {
            user.setCourseSelected3(courseIdStr);
            user.setCourse3Progress(0);
        } else {
            return false; // No empty slots
        }

        userService.updateUser(user);
        return true;
    }
}
