package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import com.example.demo.service.RecommendationService;
import com.example.demo.dto.CourseRecommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class RecommendationController {

    @Autowired
    private UserService userService;

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/course/recommendations")
    public String showRecommendations(Model model, Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userService.findByEmail(userEmail);
        
        List<CourseRecommendation> recommendations = recommendationService.getRecommendationsForUser(user);
        
        // Check for empty slots
        boolean hasEmptySlot = user.getCourseSelected1() == null || 
                             user.getCourseSelected2() == null || 
                             user.getCourseSelected3() == null;
                             
        // Check for completed courses in course_progress table
        boolean hasCompletedCourse = recommendationService.isCompletedCourse(user, user.getCourseSelected1()) ||
                                   recommendationService.isCompletedCourse(user, user.getCourseSelected2()) ||
                                   recommendationService.isCompletedCourse(user, user.getCourseSelected3());
        
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("hasEmptySlot", hasEmptySlot);
        model.addAttribute("hasCompletedCourse", hasCompletedCourse);
        model.addAttribute("courseSlot1", user.getCourseSelected1());
        model.addAttribute("courseSlot2", user.getCourseSelected2());
        model.addAttribute("courseSlot3", user.getCourseSelected3());
        
        return "recommendations";
    }

    @PostMapping("/course/select-course")
    public String selectCourse(@RequestParam("courseId") Long courseId, 
                             @RequestParam(value = "replaceCompleted", required = false) Boolean replaceCompleted,
                             Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userService.findByEmail(userEmail);
        
        boolean success = recommendationService.assignCourseToUser(user, courseId, replaceCompleted);
        
        if (!success) {
            return "redirect:/course/recommendations?error=maxCourses";
        }
        
        return "redirect:/home";
    }
}
