package com.example.demo.web;

import com.example.demo.service.LearningPathVerificationService;
import com.example.demo.service.RecommendationService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class LearningPathController {

    private final LearningPathVerificationService learningPathVerificationService;
    private final RecommendationService recommendationService;
    private final UserService userService;

    @Autowired
    public LearningPathController(
        LearningPathVerificationService learningPathVerificationService,
        RecommendationService recommendationService,
        UserService userService
    ) {
        this.learningPathVerificationService = learningPathVerificationService;
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    @PostMapping("/verify-learning-paths")
    public ResponseEntity<Boolean> verifyLearningPaths(@RequestBody Map<String, Object> payload, Authentication authentication) {
        Long userId = Long.parseLong(payload.get("userId").toString());
        
        // Convert course IDs to integers (supports both numeric and string JSON values)
        List<Integer> courseIds = ((List<?>) payload.get("courseIds")).stream()
            .map(Object::toString)
            .map(Integer::parseInt)
            .collect(Collectors.toList());

        // Verify and fetch learning paths only for selected courses
        boolean allPathsExist = learningPathVerificationService.verifyAndFetchLearningPaths(userId, courseIds);

        if (allPathsExist) {
            // If all paths exist, assign courses to the user
            String userEmail = authentication.getName();
            var user = userService.findByEmail(userEmail);

            // Assign each course to the user
            for (Integer courseId : courseIds) {
                recommendationService.assignCourseToUser(user, courseId.longValue(), false);
            }
        }

        return ResponseEntity.ok(allPathsExist);
    }
}
