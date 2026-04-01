package com.example.demo.web;

import com.example.demo.model.CourseProgress;
import com.example.demo.model.User;
import com.example.demo.repository.CourseProgressRepository;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
public class ProgressController {

    private final String BASE_PATH = "data/learning_paths/content";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private CourseProgressRepository courseProgressRepository;
    
    @Autowired
    private UserService userService;

    @PostMapping("/progress/init/{userId}/{courseId}/{activityId}")
    public ResponseEntity<?> initializeProgress(
            @PathVariable Long userId,
            @PathVariable String courseId,
            @PathVariable String activityId) {
        try {
            String progressPath = String.format("%s/%d/%s/%s", BASE_PATH, userId, courseId, activityId);
            File progressDir = new File(progressPath);
            progressDir.mkdirs();

            File progressFile = new File(progressDir, "progress.json");
            if (!progressFile.exists()) {
                Map<String, Object> progress = new HashMap<>();
                progress.put("completed", false);
                objectMapper.writeValue(progressFile, progress);
            }
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to initialize progress");
        }
    }

    @PostMapping("/progress/complete/{userId}/{courseId}/{activityId}")
    public ResponseEntity<?> markComplete(
            @PathVariable Long userId,
            @PathVariable String courseId,
            @PathVariable String activityId) {
        try {
            String progressPath = String.format("%s/%d/%s/%s", BASE_PATH, userId, courseId, activityId);
            File progressFile = new File(new File(progressPath), "progress.json");

            Map<String, Object> progress = new HashMap<>();
            progress.put("completed", true);
            objectMapper.writeValue(progressFile, progress);

            // Update course progress in database
            updateCourseProgress(userId, courseId);
            
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to mark as complete");
        }
    }

    @GetMapping("/progress/course/{userId}/{courseId}")
    public ResponseEntity<?> getCourseProgress(
            @PathVariable Long userId,
            @PathVariable String courseId) {
        try {
            String coursePath = String.format("%s/%d/%s", BASE_PATH, userId, courseId);
            Map<String, Boolean> activities = new HashMap<>();

            Path courseDir = Paths.get(coursePath);
            if (!Files.exists(courseDir)) {
                updateCourseProgress(userId, courseId);
                return ResponseEntity.ok(Map.of("activities", activities));
            }

            try (Stream<Path> paths = Files.walk(courseDir)) {
                paths.filter(path -> path.toString().endsWith("progress.json"))
                     .forEach(path -> {
                         try {
                             Map<?, ?> progress = objectMapper.readValue(path.toFile(), Map.class);
                             String activityId = path.getParent().getFileName().toString();
                             activities.put(activityId, (Boolean) progress.get("completed"));
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     });
            }

            // Update course progress in database
            updateCourseProgress(userId, courseId);
            
            return ResponseEntity.ok(Map.of("activities", activities));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to get course progress");
        }
    }

    @PostMapping("/progress/update-total/{userId}/{courseId}")
    public ResponseEntity<?> updateTotalProgress(
            @PathVariable Long userId,
            @PathVariable String courseId,
            @RequestBody Map<String, Integer> progressData) {
        try {
            CourseProgress courseProgress = courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElse(new CourseProgress());

            courseProgress.setUserId(userId);
            courseProgress.setCourseId(courseId);
            courseProgress.setTotalActivities(progressData.get("totalActivities"));
            courseProgress.setCompletedActivities(progressData.get("completedActivities"));
            courseProgress.setProgressPercentage(progressData.get("progressPercentage"));
            courseProgress.setLastUpdated(LocalDateTime.now());

            courseProgressRepository.save(courseProgress);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to update total progress");
        }
    }

    private void updateCourseProgress(Long userId, String courseId) {
        try {
            String coursePath = String.format("%s/%d/%s", BASE_PATH, userId, courseId);
            int[] counts = {0, 0}; // [totalActivities, completedActivities]

            Path courseDir = Paths.get(coursePath);
            if (!Files.exists(courseDir)) {
                CourseProgress courseProgress = courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                        .orElse(new CourseProgress(userId, courseId));

                courseProgress.setTotalActivities(0);
                courseProgress.setCompletedActivities(0);
                courseProgress.setProgressPercentage(0);
                courseProgress.setLastUpdated(LocalDateTime.now());
                courseProgressRepository.save(courseProgress);
                return;
            }

            try (Stream<Path> paths = Files.walk(courseDir)) {
                paths.filter(path -> path.toString().endsWith("progress.json"))
                     .forEach(path -> {
                         try {
                             Map<?, ?> progress = objectMapper.readValue(path.toFile(), Map.class);
                             counts[0]++; // Increment total activities
                             if ((Boolean) progress.get("completed")) {
                                 counts[1]++; // Increment completed activities
                             }
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     });
            }

            // Calculate percentage
            int percentage = counts[0] > 0 ? (counts[1] * 100) / counts[0] : 0;

            // Update database
            CourseProgress courseProgress = courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
                    .orElse(new CourseProgress(userId, courseId));

            courseProgress.setTotalActivities(counts[0]);
            courseProgress.setCompletedActivities(counts[1]);
            courseProgress.setProgressPercentage(percentage);
            courseProgress.setLastUpdated(LocalDateTime.now());

            courseProgressRepository.save(courseProgress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/progress-stats")
    public ResponseEntity<?> getProgressStats(@AuthenticationPrincipal UserDetails principal) {
        try {
            User user = userService.findByEmail(principal.getUsername());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            int completedCourses = 0;
            int inProgressCourses = 0;

            List<CourseProgress> progresses = courseProgressRepository.findByUserId(user.getId());
            for (CourseProgress progress : progresses) {
                if (progress.getProgressPercentage() == 100) {
                    completedCourses++;
                } else if (progress.getProgressPercentage() > 0) {
                    inProgressCourses++;
                }
            }

            Map<String, Integer> stats = new HashMap<>();
            stats.put("completedCourses", completedCourses);
            stats.put("inProgressCourses", inProgressCourses);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace(); // Add stack trace for debugging
            return ResponseEntity.internalServerError().body("Failed to fetch progress stats: " + e.getMessage());
        }
    }

    @GetMapping("/course/progress/status")
    public ResponseEntity<?> getCourseProgressStatus(@AuthenticationPrincipal UserDetails principal) {
        try {
            User user = userService.findByEmail(principal.getUsername());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Map<String, Object> courseProgress = new HashMap<>();
            List<CourseProgress> progresses = courseProgressRepository.findByUserId(user.getId());

            for (CourseProgress progress : progresses) {
                // Use the exact course ID from the database
                courseProgress.put(progress.getCourseId(), progress.getProgressPercentage());
            }

            System.out.println("Course progress data: " + courseProgress);
            return ResponseEntity.ok(courseProgress);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to fetch course progress status");
        }
    }
}
