package com.example.demo.web;

import com.example.demo.model.Course;
import com.example.demo.model.CourseProgress;
import com.example.demo.model.User;
import com.example.demo.service.CourseProgressService;
import com.example.demo.service.UserService;
import com.example.demo.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/dashboard/api")
public class CourseProgressController {

    private static final Logger logger = LoggerFactory.getLogger(CourseProgressController.class);

    @Autowired
    private CourseProgressService courseProgressService;

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @GetMapping("/course-progress")
    public ResponseEntity<List<Map<String, Object>>> getCourseProgress(Principal principal) {
        try {
            if (principal == null) {
                logger.error("No authenticated user found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userService.findByEmail(principal.getName());
            if (user == null) {
                logger.error("User not found for email: {}", principal.getName());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            List<CourseProgress> progressList = courseProgressService.findByUserId(user.getId());
            logger.info("Found {} progress records for user {}", progressList.size(), user.getId());
            
            // Convert to list of maps to include course title
            List<Map<String, Object>> result = progressList.stream()
                .map(progress -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("courseId", progress.getCourseId());
                    map.put("progressPercentage", progress.getProgressPercentage());
                    map.put("lastUpdated", progress.getLastUpdated());
                    
                    // Get course title from courseService
                    String courseTitle = courseService.getCourseTitle(progress.getCourseId())
                        .orElse("Unknown Course");
                    map.put("courseTitle", courseTitle);
                    
                    return map;
                })
                .toList();
            
            logger.info("Course progress for user {}: {}", user.getId(), result);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error getting course progress: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/course-progress/stats")
    public ResponseEntity<Map<String, Integer>> getCourseProgressStats(Principal principal) {
        try {
            if (principal == null) {
                logger.error("No authenticated user found");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userService.findByEmail(principal.getName());
            if (user == null) {
                logger.error("User not found for email: {}", principal.getName());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            List<CourseProgress> progressList = courseProgressService.findByUserId(user.getId());
            logger.info("Found {} progress records for user {}", progressList.size(), user.getId());
            
            int completed = 0;
            int ongoing = 0;
            
            for (CourseProgress progress : progressList) {
                if (progress.getProgressPercentage() == 100) {
                    completed++;
                } else {
                    ongoing++;
                }
            }
            
            // Calculate not started courses
            int notStarted = courseService.getNotStartedCoursesCount(user.getId(), progressList);
            
            Map<String, Integer> stats = new HashMap<>();
            stats.put("completed", completed);
            stats.put("ongoing", ongoing);
            stats.put("notStarted", notStarted);
            
            logger.info("Course stats for user {}: completed={}, ongoing={}, notStarted={}", 
                user.getId(), completed, ongoing, notStarted);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error getting course stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
