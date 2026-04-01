package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.web.dto.UserRegistrationDto;
import com.example.demo.web.dto.UserEditProfileDto;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.ArrayList;
import java.util.List;

public interface UserService extends UserDetailsService {
    User save(UserRegistrationDto registrationDto);
    String generateOtp(String email);
    boolean verifyOtp(String email, String otp);
    User findByEmail(String email);
    User findByUsername(String username);
    User findById(Long id);  
    void updateUser(User user);
    void updatePassword(User user, String newPassword);
    User editProfile(Long userId, UserEditProfileDto editProfileDto);
    default List<String> getRecommendedCourses(User user) {
        List<String> recommendedCourses = new ArrayList<>();
        
        // Add some default course recommendations
        recommendedCourses.add("Java Programming");
        recommendedCourses.add("Web Development");
        recommendedCourses.add("Data Science");
        recommendedCourses.add("Machine Learning");
        recommendedCourses.add("Cloud Computing");
        
        // Filter out courses the user is already enrolled in
        recommendedCourses.removeIf(course -> 
            course.equals(user.getCourseSelected1()) ||
            course.equals(user.getCourseSelected2()) ||
            course.equals(user.getCourseSelected3())
        );
        
        return recommendedCourses.subList(0, Math.min(3, recommendedCourses.size()));
    }
}
