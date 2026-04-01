package com.example.demo.web;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import com.example.demo.service.UserServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/forgot-password")
public class ForgotPasswordController {

    private final UserService userService;

    public ForgotPasswordController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String showForgotPasswordForm() {
        return "forgotpass";
    }

    @PostMapping("/generate-otp")
    @ResponseBody
    public String generateOtp(@RequestParam String email, @RequestParam String otp) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return "Email not found";
        }
        ((UserServiceImpl) userService).storeOtp(email, otp);
        return "OTP stored successfully";
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public boolean verifyOtp(@RequestParam String email, @RequestParam String otp) {
        return userService.verifyOtp(email, otp);
    }

    @PostMapping("/update-password")
    @ResponseBody
    public String updatePassword(@RequestParam String email, @RequestParam String password) {
        User user = userService.findByEmail(email);
        if (user != null) {
            userService.updatePassword(user, password);
            return "Password updated successfully";
        }
        return "Failed to update password";
    }
}
