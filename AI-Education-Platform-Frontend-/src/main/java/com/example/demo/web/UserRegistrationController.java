package com.example.demo.web;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import com.example.demo.service.UserServiceImpl;
import com.example.demo.web.dto.UserRegistrationDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/registration")
public class UserRegistrationController {

    private final UserService userService;

    public UserRegistrationController(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute("user")
    public UserRegistrationDto userRegistrationDto() {
        return new UserRegistrationDto();
    }

    @GetMapping
    public String showRegistrationForm() {
        return "registration";
    }

    @PostMapping("/generate-otp")
    @ResponseBody
    public String generateOtp(@RequestParam String email, @RequestParam String otp) {
        ((UserServiceImpl) userService).storeOtp(email, otp);
        return "OTP stored successfully";
    }

    @PostMapping("/verify-otp")
    @ResponseBody
    public boolean verifyOtp(@RequestParam String email, @RequestParam String otp) {
        return userService.verifyOtp(email, otp);
    }

    @PostMapping("/check-email")
    @ResponseBody
    public boolean checkEmailExists(@RequestParam String email) {
        User existingUser = userService.findByEmail(email);
        return existingUser != null;
    }

    @PostMapping
    public String registerUserAccount(@ModelAttribute("user") UserRegistrationDto registrationDto, Model model) {
        try {
            userService.save(registrationDto);
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "registration";
        }
    }
}
