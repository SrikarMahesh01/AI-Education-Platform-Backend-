package com.example.demo.web;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.User;
import com.example.demo.model.QuizResult;
import com.example.demo.model.QuizAnswer;
import com.example.demo.service.UserService;
import com.example.demo.service.QuizService;
import com.example.demo.service.DataExportService;

@Controller
@RequestMapping("/quiz")
public class QuizController {

    private final UserService userService;
    private final QuizService quizService;
    private final DataExportService dataExportService;

    @Autowired
    public QuizController(UserService userService, QuizService quizService, DataExportService dataExportService) {
        this.userService = userService;
        this.quizService = quizService;
        this.dataExportService = dataExportService;
    }

    static class QuizSubmission {
        private int score;
        private int totalQuestions;
        private List<QuizAnswer> answers;

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public List<QuizAnswer> getAnswers() { return answers; }
        public void setAnswers(List<QuizAnswer> answers) { this.answers = answers; }
    }

    @GetMapping
    public String showQuizPage(Model model, Principal principal, HttpServletResponse response) {
        User user = userService.findByEmail(principal.getName());
        
        // Check if survey is not completed
        if (user.getSurveyCompleted() == 0) {
            return "redirect:/survey";
        }
        
        // Check if quiz is already completed
        if (user.getQuizCompleted() == 1) {
            // If quiz is completed, check course selection
            if (user.getCourseSelected1() != null && !user.getCourseSelected1().isEmpty()) {
                return "redirect:/home";
            } else {
                return "redirect:/index";
            }
        }

        model.addAttribute("user", user);
        return "quiz";
    }

    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<?> submitQuizResult(@RequestBody QuizSubmission submission, Principal principal) {
        try {
            User user = userService.findByEmail(principal.getName());
            if (user != null) {
                // Check if quiz is already completed (prevent double submission)
                if (user.getQuizCompleted() == 1) {
                    return ResponseEntity.badRequest().body("Quiz already completed");
                }

                QuizResult result = quizService.saveQuizResult(
                    user,
                    submission.getScore(),
                    submission.getTotalQuestions(),
                    submission.getAnswers()
                );

                // Update user's quiz completion status
                user.setQuizCompleted(1);
                userService.updateUser(user);

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Quiz completed successfully",
                    "redirectUrl", "/index"
                ));
            }
            return ResponseEntity.badRequest().body("User not found");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/results")
    @ResponseBody
    public ResponseEntity<?> getQuizResults(Principal principal) {
        User user = userService.findByEmail(principal.getName());
        List<QuizResult> results = quizService.getUserResults(user);
        return ResponseEntity.ok().body(results);
    }

    @GetMapping("/top-results")
    @ResponseBody
    public ResponseEntity<?> getTopResults() {
        List<QuizResult> topResults = quizService.getTopResults();
        return ResponseEntity.ok().body(topResults);
    }

    @GetMapping("/results/{quizId}")
    @ResponseBody
    public ResponseEntity<?> getQuizDetails(@PathVariable Long quizId) {
        QuizResult quizResult = quizService.getQuizResult(quizId);
        if (quizResult != null) {
            List<QuizAnswer> answers = quizService.getQuizAnswers(quizResult);
            return ResponseEntity.ok().body(answers);
        }
        return ResponseEntity.notFound().build();
    }
}
