package com.example.demo.web;

import com.example.demo.model.SurveyResponse;
import com.example.demo.model.User;
import com.example.demo.repository.SurveyRepository;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

@Controller
@RequestMapping("/survey")
public class SurveyController {

    private final UserService userService;
    private final SurveyRepository surveyRepository;

    @Autowired
    public SurveyController(UserService userService, SurveyRepository surveyRepository) {
        this.userService = userService;
        this.surveyRepository = surveyRepository;
    }

    @GetMapping
    public String showSurveyForm(Model model, Principal principal) {
        // Fetch the user by email
        User user = userService.findByEmail(principal.getName());
        
        // Check if survey is already completed
        if (user.getSurveyCompleted() == 1) {
            // If survey completed, check quiz
            if (user.getQuizCompleted() == 0) {
                return "redirect:/quiz";
            } else if (user.getCourseSelected1() == null || user.getCourseSelected1().isEmpty()) {
                return "redirect:/index";
            } else {
                return "redirect:/home";
            }
        }

        // Show survey form if not completed
        model.addAttribute("user", user);
        model.addAttribute("surveyResponse", new SurveyResponse());
        return "survey";
    }

    @PostMapping
    public String submitSurvey(@ModelAttribute SurveyResponse surveyResponse, Principal principal, HttpServletResponse response) {
        User user = userService.findByEmail(principal.getName());
        if (user != null) {
            // Check if survey is already completed (double submission prevention)
            if (user.getSurveyCompleted() == 1) {
                response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
                return "redirect:/quiz";
            }

            surveyResponse.setUser(user);
            user.setSurveyCompleted(1);
            userService.updateUser(user);
            surveyRepository.save(surveyResponse);

            // Add cache control headers
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        return "redirect:/quiz";
    }
}
