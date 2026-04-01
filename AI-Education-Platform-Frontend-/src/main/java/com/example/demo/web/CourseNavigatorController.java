package com.example.demo.web;

import com.example.demo.model.User;
import com.example.demo.service.ProfilePictureService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

@Controller
@RequestMapping("/course")
public class CourseNavigatorController {
    private static final Logger logger = Logger.getLogger(CourseNavigatorController.class.getName());

    @Autowired
    private UserService userService;

    @Autowired
    private ProfilePictureService profilePictureService;

    private final String DATA_JSON_PATH = "DATA JSON/";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/view/{courseName}")
    public String showCourse(@PathVariable String courseName, Model model, Principal principal) {
        try {
            // Get current user
            User user = userService.findByEmail(principal.getName());
            
            // Verify user has access to this course
            if (!isUserEnrolledInCourse(user, courseName)) {
                return "redirect:/home?error=Course not found";
            }
            
            model.addAttribute("user", user);
            model.addAttribute("courseName", courseName);

            profilePictureService.getProfilePicture(user.getId()).ifPresent(profilePicture -> {
                String base64Picture = Base64.getEncoder().encodeToString(profilePicture.getPictureData());
                model.addAttribute("base64ProfilePicture", base64Picture);
                model.addAttribute("profilePictureType", profilePicture.getContentType());
            });

            // Load course structure
            String structureFileName = "MX-SX-AX.json";
            File structureFile = new File(DATA_JSON_PATH + structureFileName);
            
            if (!structureFile.exists()) {
                logger.warning("Course structure file not found: " + structureFileName);
                return "redirect:/home?error=Course content not available";
            }

            Map<String, Object> courseStructure = objectMapper.readValue(structureFile, Map.class);
            model.addAttribute("courseStructure", courseStructure);

            return "course-navigator";
        } catch (IOException e) {
            logger.severe("Error loading course structure: " + e.getMessage());
            return "redirect:/home?error=Error loading course content";
        }
    }

    @GetMapping("/{courseName}/content/{componentId}")
    @ResponseBody
    public ResponseEntity<?> getComponentContent(@PathVariable String courseName,
                                               @PathVariable String componentId,
                                               Principal principal) {
        try {
            User user = userService.findByEmail(principal.getName());
            
            // Verify user has access to this course
            if (!isUserEnrolledInCourse(user, courseName)) {
                return ResponseEntity.badRequest().body("Access denied");
            }

            String contentFileName = "MX-SX-AX-CX.json";
            File contentFile = new File(DATA_JSON_PATH + contentFileName);
            
            if (!contentFile.exists()) {
                logger.warning("Content file not found: " + contentFileName);
                return ResponseEntity.badRequest().body("Content file not found");
            }

            Map<String, Object> content = objectMapper.readValue(contentFile, Map.class);
            Map<String, Object> componentContent = (Map<String, Object>) content.get("component_content");
            
            if (componentContent == null) {
                logger.warning("No component_content found in " + contentFileName);
                return ResponseEntity.badRequest().body("Component content structure invalid");
            }

            String contentId = (String) componentContent.get("id");
            if (contentId == null || !componentId.equals(contentId)) {
                // Try to find the content in the content array
                Object[] contentArray = (Object[]) componentContent.get("content");
                if (contentArray != null) {
                    for (Object item : contentArray) {
                        Map<String, Object> contentItem = (Map<String, Object>) item;
                        if (componentId.equals(contentItem.get("id"))) {
                            return ResponseEntity.ok(contentItem);
                        }
                    }
                }
                logger.warning("Component ID mismatch. Expected: " + componentId + ", Found: " + contentId);
                return ResponseEntity.badRequest().body("Component not found");
            }

            return ResponseEntity.ok(content);
        } catch (IOException e) {
            logger.severe("Error loading component content: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error loading content");
        } catch (Exception e) {
            logger.severe("Unexpected error: " + e.getMessage());
            return ResponseEntity.badRequest().body("An unexpected error occurred");
        }
    }

    private boolean isUserEnrolledInCourse(User user, String courseName) {
        if (user == null || courseName == null) {
            return false;
        }
        return courseName.equals(user.getCourseSelected1()) ||
               courseName.equals(user.getCourseSelected2()) ||
               courseName.equals(user.getCourseSelected3());
    }
}
