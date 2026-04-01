package com.example.demo.web;

import com.example.demo.model.Course;
import com.example.demo.model.CourseProgress;
import com.example.demo.model.LearningPath;
import com.example.demo.model.ProfilePicture;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.CompletedCoursesService;
import com.example.demo.service.CourseRecommendationFetchService;
import com.example.demo.service.CourseRecommendationService;
import com.example.demo.service.CourseService;
import com.example.demo.service.DataExportService;
import com.example.demo.service.FeedbackService;
import com.example.demo.service.ProfilePictureService;
import com.example.demo.service.UserService;
import com.example.demo.repository.CourseProgressRepository;
import com.example.demo.web.dto.UserEditProfileDto;
import com.example.demo.web.dto.UserRegistrationDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import javax.validation.Valid;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.stream.StreamSupport;
import java.util.Arrays;
import java.util.Iterator;
import java.io.File;

@Controller
public class MainController {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    private final UserService userService;
    private final DataExportService dataExportService;
    private final CourseRecommendationService courseRecommendationService;
    private final CourseRecommendationFetchService courseRecommendationFetchService;
    private final ProfilePictureService profilePictureService;
    private final CourseService courseService;
    private final ResourceLoader resourceLoader;
    private final CourseProgressRepository courseProgressRepository;
    private final CompletedCoursesService completedCoursesService;
    private final FeedbackService feedbackService;
    
    @Autowired
    public MainController(UserService userService, 
                         DataExportService dataExportService,
                         CourseRecommendationService courseRecommendationService,
                         CourseRecommendationFetchService courseRecommendationFetchService,
                         ProfilePictureService profilePictureService,
                         CourseService courseService,
                         CompletedCoursesService completedCoursesService,
                         ResourceLoader resourceLoader,
                         CourseProgressRepository courseProgressRepository,
                         FeedbackService feedbackService) {
        this.userService = userService;
        this.dataExportService = dataExportService;
        this.courseRecommendationService = courseRecommendationService;
        this.courseRecommendationFetchService = courseRecommendationFetchService;
        this.profilePictureService = profilePictureService;
        this.courseService = courseService;
        this.completedCoursesService = completedCoursesService;
        this.resourceLoader = resourceLoader;
        this.courseProgressRepository = courseProgressRepository;
        this.feedbackService = feedbackService;
    }
    
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String root(Principal principal) {
        if (principal == null) {
            return "forward:/index.html";
        }

        User user = userService.findByEmail(principal.getName());
        
        // Check 1: Survey completion
        if (user.getSurveyCompleted() == 0) {
            return "redirect:/survey";
        }
        
        // Check 2: Quiz completion
        if (user.getQuizCompleted() == 0) {
            return "redirect:/quiz";
        }
        
        // Check 3: Course selection
        if (user.getCourseSelected1() == null || user.getCourseSelected1().isEmpty()) {
            // Fetch course suggestions for the user
            courseRecommendationFetchService.fetchAndSaveCourseRecommendations(user.getId());
            return "redirect:/index";
        }
        
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model, Principal principal, @RequestParam(required = false) String error) {
        try {
            if (principal == null) {
                return "redirect:/login";
            }

            User user = userService.findByEmail(principal.getName());
            if (user == null) {
                return "redirect:/login";
            }

            if (error != null && !error.isEmpty()) {
                model.addAttribute("errorMessage", error);
            }

            // Add profile picture to model if exists
            profilePictureService.getProfilePicture(user.getId()).ifPresent(profilePicture -> {
                String base64Picture = Base64.getEncoder().encodeToString(profilePicture.getPictureData());
                model.addAttribute("base64ProfilePicture", base64Picture);
                model.addAttribute("profilePictureType", profilePicture.getContentType());
            });

            // Get course progress for achievements
            List<CourseProgress> progressList = courseProgressRepository.findByUserId(user.getId());
            List<Map<String, Object>> courseProgressList = progressList.stream()
                .filter(progress -> progress.getProgressPercentage() == 100)
                .map(progress -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("courseId", progress.getCourseId());
                    map.put("progressPercentage", progress.getProgressPercentage());
                    map.put("lastUpdated", progress.getLastUpdated());
                    map.put("courseTitle", courseService.getCourseTitle(progress.getCourseId()).orElse("Unknown Course"));
                    return map;
                })
                .collect(Collectors.toList());
            
            // Generate completed courses JSON file
            try {
                logger.info("Generating completed courses JSON for user ID: {}", user.getId());
                completedCoursesService.generateCompletedCoursesJson(user.getId());
                
                // Read the completed courses file
                File projectRoot = new File(System.getProperty("user.dir"));
                File completedFile = new File(projectRoot, "src/main/resources/static/data/data/completed_" + user.getId() + ".json");
                
                if (completedFile.exists()) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    List<Map<String, Object>> achievementsList = objectMapper.readValue(completedFile, List.class);
                    model.addAttribute("completedCourses", achievementsList);
                    logger.info("Added {} completed courses to model", achievementsList.size());
                } else {
                    logger.warn("No completed courses file found at: {}", completedFile.getAbsolutePath());
                    model.addAttribute("completedCourses", new ArrayList<>());
                    model.addAttribute("certificateError", "No completed courses found. Please complete some courses.");
                }
            } catch (Exception e) {
                logger.error("Error handling completed courses: {}", e.getMessage(), e);
                model.addAttribute("completedCourses", new ArrayList<>());
                model.addAttribute("certificateError", "Error loading certificates. Please try again later.");
            }
            
            model.addAttribute("courseProgress", courseProgressList);

            List<Course> selectedCourses = new ArrayList<>();
            Map<String, LearningPath> coursePaths = new HashMap<>();
            List<Map<String, Object>> recommendedCourses = new ArrayList<>();
            
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String userId = user.getId().toString();
                Resource suggestionsResource = resourceLoader.getResource(String.format("classpath:static/data/data/suggest_courses_%s.json", userId));
                
                if (!suggestionsResource.exists()) {
                    throw new Exception("Course suggestions file not found");
                }

                JsonNode rootNode = objectMapper.readTree(suggestionsResource.getInputStream());
                JsonNode recommendationsNode = rootNode.path("recommendations");

                // Load all recommended courses
                recommendationsNode.forEach(courseNode -> {
                    Map<String, Object> course = new HashMap<>();
                    course.put("id", courseNode.path("id").asText());
                    course.put("title", courseNode.path("title").asText());
                    course.put("description", courseNode.path("description").asText());
                    recommendedCourses.add(course);
                });
                
                // Add selected courses and load their learning paths
                if (user.getCourseSelected1() != null) {
                    if (findAndAddCourseFromJson(recommendationsNode, user.getCourseSelected1(), selectedCourses)) {
                        loadLearningPath(userId, user.getCourseSelected1(), coursePaths);
                    }
                }
                if (user.getCourseSelected2() != null) {
                    if (findAndAddCourseFromJson(recommendationsNode, user.getCourseSelected2(), selectedCourses)) {
                        loadLearningPath(userId, user.getCourseSelected2(), coursePaths);
                    }
                }
                if (user.getCourseSelected3() != null) {
                    if (findAndAddCourseFromJson(recommendationsNode, user.getCourseSelected3(), selectedCourses)) {
                        loadLearningPath(userId, user.getCourseSelected3(), coursePaths);
                    }
                }

                if (selectedCourses.isEmpty()) {
                    logger.error("No courses found for user {} with selected course IDs: {}, {}, {}",
                        userId, user.getCourseSelected1(), user.getCourseSelected2(), user.getCourseSelected3());
                    model.addAttribute("errorMessage", "Course not found. Please select courses again.");
                }
            } catch (Exception e) {
                logger.error("Error loading courses for user {}: {}", user.getId(), e.getMessage());
                model.addAttribute("errorMessage", "Error loading courses. Please try again.");
            }
            
            model.addAttribute("selectedCourses", selectedCourses);
            model.addAttribute("coursePaths", coursePaths);
            model.addAttribute("recommendedCourses", recommendedCourses);
            model.addAttribute("user", user);
            
            return "home";
        } catch (Exception e) {
            logger.error("Error in home controller", e);
            model.addAttribute("errorMessage", "An error occurred while loading the dashboard");
            return "home";
        }
    }

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        profilePictureService.getProfilePicture(user.getId()).ifPresent(profilePicture -> {
            String base64Picture = Base64.getEncoder().encodeToString(profilePicture.getPictureData());
            model.addAttribute("base64ProfilePicture", base64Picture);
            model.addAttribute("profilePictureType", profilePicture.getContentType());
        });

        model.addAttribute("user", user);
        return "profile";
    }

    private void loadLearningPath(String userId, String courseId, Map<String, LearningPath> coursePaths) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String pathFilename = String.format("static/data/data/learning_paths/%s_%s_path.json", userId, courseId);
            Resource pathResource = resourceLoader.getResource(String.format("classpath:%s", pathFilename));
            
            logger.info("Attempting to load learning path: {}", pathFilename);
            
            if (pathResource.exists()) {
                logger.info("Found learning path file for course {}", courseId);
                LearningPath path = objectMapper.readValue(pathResource.getInputStream(), LearningPath.class);
                coursePaths.put(courseId, path);
                logger.info("Successfully loaded learning path for course {}. Number of modules: {}", 
                    courseId, path.getModules().size());
            } else {
                logger.warn("Learning path file not found: {}", pathFilename);
            }
        } catch (Exception e) {
            logger.error("Error loading learning path for course {}: {}", courseId, e.getMessage(), e);
        }
    }

    private boolean findAndAddCourseFromJson(JsonNode recommendationsNode, String courseId, List<Course> selectedCourses) {
        for (JsonNode courseNode : recommendationsNode) {
            if (courseNode.path("id").asText().equals(courseId)) {
                Course course = new Course();
                course.setId(courseNode.path("id").asLong());
                course.setTitle(courseNode.path("title").asText());
                course.setDescription(courseNode.path("description").asText());
                course.setConfidenceScore(courseNode.path("confidence_score").asInt());
                selectedCourses.add(course);
                return true;
            }
        }
        return false;
    }

    @GetMapping("/index")
    public String index(Model model, Principal principal) {
        User user = userService.findByEmail(principal.getName());
        
        // Redirect if prerequisites aren't met
        if (user.getSurveyCompleted() == 0) {
            return "redirect:/survey";
        }
        if (user.getQuizCompleted() == 0) {
            return "redirect:/quiz";
        }
        
        // If courses already selected, go to home
        if (user.getCourseSelected1() != null && !user.getCourseSelected1().isEmpty()) {
            return "redirect:/home";
        }

        // Ensure course recommendations are generated
        try {
            courseRecommendationFetchService.fetchAndSaveCourseRecommendations(user.getId());
        } catch (Exception e) {
            logger.error("Error generating course recommendations for user {}: {}", user.getId(), e.getMessage());
        }

        // Check if quiz and survey are completed
        if (user.getQuizCompleted() == 1 && user.getSurveyCompleted() == 1) {
            try {
                // Load user-specific course suggestions file
                ObjectMapper objectMapper = new ObjectMapper();
                String userId = user.getId().toString();
                Resource suggestionsResource = resourceLoader.getResource(String.format("classpath:static/data/data/suggest_courses_%s.json", userId));
                
                JsonNode rootNode = objectMapper.readTree(suggestionsResource.getInputStream());
                JsonNode recommendationsNode = rootNode.path("recommendations");
                
                // Convert recommendations to list of maps
                List<Map<String, Object>> recommendations = new ArrayList<>();
                for (JsonNode rec : recommendationsNode) {
                    Map<String, Object> course = new HashMap<>();
                    course.put("id", rec.path("id").asText());
                    course.put("title", rec.path("title").asText());
                    course.put("description", rec.path("description").asText());
                    course.put("confidence_score", rec.path("confidence_score").asDouble());
                    recommendations.add(course);
                }
                
                model.addAttribute("recommendations", recommendations);
            } catch (Exception e) {
                logger.error("Error loading course suggestions for user {}: {}", user.getId(), e.getMessage());
                model.addAttribute("errorMessage", "Unable to load course suggestions. Please try again later.");
            }
        }
        
        // Add user and userId to the model
        model.addAttribute("user", user);
        model.addAttribute("userId", user.getId().toString());
        return "index";
    }

    @GetMapping("/export/quiz-answers")
    @ResponseBody
    public ResponseEntity<?> exportQuizAnswers() {
        try {
            dataExportService.exportQuizAnswers();
            return ResponseEntity.ok(Map.of("success", true, "message", "Quiz answers exported successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    @GetMapping("/edit-profile")
    public String showEditProfileForm(Model model, Principal principal) {
        try {
            User user = userService.findByEmail(principal.getName());
            
            // Create and populate UserEditProfileDto
            UserEditProfileDto editProfileDto = new UserEditProfileDto();
            editProfileDto.setFirstName(user.getFirstName());
            editProfileDto.setLastName(user.getLastName());
            editProfileDto.setDateOfBirth(user.getDateOfBirth());
            editProfileDto.setCountry(user.getCountry());
            editProfileDto.setAddress(user.getAddress());
            editProfileDto.setSex(user.getSex());
            editProfileDto.setPhoneNumber(user.getPhoneNumber());

            // Get profile picture if exists
            profilePictureService.getProfilePicture(user.getId()).ifPresent(profilePicture -> {
                String base64Picture = Base64.getEncoder().encodeToString(profilePicture.getPictureData());
                model.addAttribute("base64ProfilePicture", base64Picture);
                model.addAttribute("profilePictureType", profilePicture.getContentType());
            });

            model.addAttribute("editProfileDto", editProfileDto);
            model.addAttribute("user", user);
            return "edit-profile";
        } catch (Exception e) {
            logger.error("Error showing edit profile form: {}", e.getMessage());
            throw e;
        }
    }

    @PostMapping("/edit-profile")
    public String updateProfile(@ModelAttribute("editProfileDto") @Valid UserEditProfileDto editProfileDto,
                              BindingResult result,
                              @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
                              Principal principal,
                              Model model) {
        User user = null;
        try {
            user = userService.findByEmail(principal.getName());
            if (user == null) {
                model.addAttribute("errorMessage", "User not found");
                return "edit-profile";
            }

            if (result.hasErrors()) {
                model.addAttribute("user", user);
                return "edit-profile";
            }

            // Update user profile using the editProfile method
            user = userService.editProfile(user.getId(), editProfileDto);

            // Save profile picture if provided
            if (profilePicture != null && !profilePicture.isEmpty()) {
                try {
                    profilePictureService.saveProfilePicture(user.getId(), profilePicture);
                } catch (IOException e) {
                    logger.error("Error uploading profile picture: {}", e.getMessage());
                    model.addAttribute("errorMessage", "Error uploading profile picture: " + e.getMessage());
                    model.addAttribute("user", user);
                    return "edit-profile";
                }
            }

            return "redirect:/home?success=profile-updated";
        } catch (Exception e) {
            logger.error("Error updating profile: {}", e.getMessage());
            model.addAttribute("errorMessage", "Error updating profile: " + e.getMessage());
            if (user != null) {
                model.addAttribute("user", user);
            }
            return "edit-profile";
        }
    }
    
    @PostMapping("/save-course-selections")
    @ResponseBody
    public ResponseEntity<?> saveCourseSelections(@RequestBody Map<String, String> selections, Principal principal) {
        try {
            User user = userService.findByEmail(principal.getName());
            
            // Log the incoming selections
            logger.info("Saving course selections for user {}: {}, {}, {}", 
                user.getEmail(),
                selections.get("courseSelected1"),
                selections.get("courseSelected2"),
                selections.get("courseSelected3"));

            // Set courses, using null for unselected
            user.setCourseSelected1(selections.containsKey("courseSelected1") ? selections.get("courseSelected1") : null);
            user.setCourseSelected2(selections.containsKey("courseSelected2") ? selections.get("courseSelected2") : null);
            user.setCourseSelected3(selections.containsKey("courseSelected3") ? selections.get("courseSelected3") : null);
            
            userService.updateUser(user);
            
            // Log after saving
            logger.info("Successfully saved course selections for user {}", user.getEmail());
            
            // Return redirect URL in response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("redirectUrl", "/home");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error saving course selections", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/courses")
    public String viewCourses(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        // Add user to model
        model.addAttribute("user", user);

        // Fetch course details for selected courses
        List<Course> selectedCourses = new ArrayList<>();
        
        try {
            // Read the user's course suggestions file
            ObjectMapper objectMapper = new ObjectMapper();
            String userId = user.getId().toString();
            Resource suggestionsResource = resourceLoader.getResource(String.format("classpath:static/data/data/suggest_courses_%s.json", userId));
            
            if (!suggestionsResource.exists()) {
                throw new Exception("Course suggestions file not found");
            }

            JsonNode rootNode = objectMapper.readTree(suggestionsResource.getInputStream());
            JsonNode recommendationsNode = rootNode.path("recommendations");
            
            // Add selected courses to the list with error logging
            boolean courseFound = false;
            if (user.getCourseSelected1() != null) {
                courseFound |= findAndAddCourseFromJson(recommendationsNode, user.getCourseSelected1(), selectedCourses);
            }
            if (user.getCourseSelected2() != null) {
                courseFound |= findAndAddCourseFromJson(recommendationsNode, user.getCourseSelected2(), selectedCourses);
            }
            if (user.getCourseSelected3() != null) {
                courseFound |= findAndAddCourseFromJson(recommendationsNode, user.getCourseSelected3(), selectedCourses);
            }

            // If no courses found, log an error
            if (!courseFound && (user.getCourseSelected1() != null || user.getCourseSelected2() != null || user.getCourseSelected3() != null)) {
                logger.error("No courses found for user {} with selected course IDs: {}, {}, {}",
                    userId, user.getCourseSelected1(), user.getCourseSelected2(), user.getCourseSelected3());
                model.addAttribute("errorMessage", "Some selected courses could not be found.");
            }
        } catch (Exception e) {
            logger.error("Error loading selected courses for user {}: {}", user.getId(), e.getMessage());
            model.addAttribute("errorMessage", "Error loading courses. Please try again.");
        }

        model.addAttribute("selectedCourses", selectedCourses);
        return "courses";
    }

    @GetMapping("/course-navigator/{courseId}")
    public String courseNavigator(@PathVariable Long courseId, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user); // Add this line to include user in model

        profilePictureService.getProfilePicture(user.getId()).ifPresent(profilePicture -> {
            String base64Picture = Base64.getEncoder().encodeToString(profilePicture.getPictureData());
            model.addAttribute("base64ProfilePicture", base64Picture);
            model.addAttribute("profilePictureType", profilePicture.getContentType());
        });

        try {
            // Read the course path file
            ObjectMapper objectMapper = new ObjectMapper();
            String userId = user.getId().toString();
            String pathFileName = String.format("static/data/data/learning_paths/%s_%s_path.json", userId, courseId);
            Resource pathResource = resourceLoader.getResource(String.format("classpath:%s", pathFileName));
            
            if (!pathResource.exists()) {
                model.addAttribute("error", "Course path data not found");
                return "course-navigator";
            }

            // Read and parse the JSON data into our model classes
            JsonNode rootNode = objectMapper.readTree(pathResource.getInputStream());
            LearningPath learningPath = objectMapper.treeToValue(rootNode.get("learning_path"), LearningPath.class);
            
            // Get course progress
            int progress = 0;
            String courseIdStr = courseId.toString();
            if (courseIdStr.equals(user.getCourseSelected1())) {
                progress = user.getCourse1Progress();
            } else if (courseIdStr.equals(user.getCourseSelected2())) {
                progress = user.getCourse2Progress();
            } else if (courseIdStr.equals(user.getCourseSelected3())) {
                progress = user.getCourse3Progress();
            }

            // Add data to model
            model.addAttribute("courseId", courseId);
            model.addAttribute("progress", progress);
            model.addAttribute("learningPath", learningPath);
            
            return "course-navigator";
            
        } catch (Exception e) {
            logger.error("Error loading course path data for user {} and course {}: {}", 
                        user.getId(), courseId, e.getMessage(), e);
            model.addAttribute("error", "Error loading course path data: " + e.getMessage());
            return "course-navigator";
        }
    }

    @GetMapping("/course/raw-navigator/{courseId}")
    public String courseRawNavigator(@PathVariable Long courseId, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Read the course path file
            ObjectMapper objectMapper = new ObjectMapper();
            String userId = user.getId().toString();
            String pathFileName = String.format("static/data/data/learning_paths/%s_%s_path.json", userId, courseId);
            Resource pathResource = resourceLoader.getResource(String.format("classpath:%s", pathFileName));
            
            if (!pathResource.exists()) {
                model.addAttribute("error", "Course path data not found");
                return "raw-json";
            }

            // Read and parse the JSON data
            JsonNode pathData = objectMapper.readTree(pathResource.getInputStream());
            
            // Convert JsonNode to pretty-printed JSON string
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
                                          .writeValueAsString(pathData);
            
            model.addAttribute("jsonData", prettyJson);
            model.addAttribute("fileName", pathFileName);
            return "raw-json";
            
        } catch (Exception e) {
            logger.error("Error loading course path data for user {} and course {}: {}", 
                        user.getId(), courseId, e.getMessage(), e);
            model.addAttribute("error", "Error loading course path data: " + e.getMessage());
            return "raw-json";
        }
    }

    @GetMapping("/course/navigator")
    public String courseNavigator(@RequestParam String courseId, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        profilePictureService.getProfilePicture(user.getId()).ifPresent(profilePicture -> {
            String base64Picture = Base64.getEncoder().encodeToString(profilePicture.getPictureData());
            model.addAttribute("base64ProfilePicture", base64Picture);
            model.addAttribute("profilePictureType", profilePicture.getContentType());
        });

        try {
            // Read the course path file
            ObjectMapper objectMapper = new ObjectMapper();
            String userId = user.getId().toString();
            JsonNode rootNode;

            // Try to load user-specific path first
            String userSpecificPath = "static/data/data/learning_paths/user_" + userId + "_path.json";
            Resource userResource = resourceLoader.getResource(String.format("classpath:%s", userSpecificPath));
            
            if (userResource.exists()) {
                rootNode = objectMapper.readTree(userResource.getInputStream());
                JsonNode coursePath = rootNode.path("users").path(userId)
                                        .path("courses").path(courseId)
                                        .path("learning_path").path("learning_path");
                
                if (coursePath.isMissingNode() || coursePath.isEmpty()) {
                    logger.error("Course {} not found in learning path for user {}", courseId, userId);
                    model.addAttribute("error", "Course not found in learning path");
                    model.addAttribute("courseId", courseId);
                    model.addAttribute("userId", userId);
                    return "course-navigator";
                }

                // Convert JsonNode to Map for easier access in Thymeleaf
                Map<String, Object> learningPathMap = objectMapper.convertValue(coursePath, Map.class);
                model.addAttribute("learningPath", learningPathMap);
                model.addAttribute("courseId", courseId);

                return "course-navigator";
            } else {
                // If user-specific path doesn't exist, use user 7's path as template
                Resource templateResource = resourceLoader.getResource("classpath:static/data/data/learning_paths/user_7_path.json");
                rootNode = objectMapper.readTree(templateResource.getInputStream());
                
                // Create a deep copy of the template for the new user
                ObjectNode newRootNode = objectMapper.createObjectNode();
                ObjectNode usersNode = newRootNode.putObject("users");
                ObjectNode userNode = usersNode.putObject(userId);
                
                // Copy courses from user 7's path
                JsonNode user7Courses = rootNode.path("users").path("7").path("courses");
                userNode.set("courses", user7Courses);
                
                // Save the new user's path
                File outputDir = new File("src/main/resources/static/data/data/learning_paths");
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }
                File outputFile = new File(outputDir, "user_" + userId + "_path.json");
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, newRootNode);
                
                rootNode = newRootNode;
            }

            // Get the course path using the correct structure
            JsonNode coursePath = rootNode.path("users").path(userId)
                                        .path("courses").path(courseId)
                                        .path("learning_path").path("learning_path");
            
            if (coursePath.isMissingNode() || coursePath.isEmpty()) {
                logger.error("Course {} not found in learning path for user {}", courseId, userId);
                model.addAttribute("error", "Course not found in learning path");
                model.addAttribute("courseId", courseId);
                model.addAttribute("userId", user.getId().toString());
                return "course-navigator";
            }

            // Convert JsonNode to Map for easier access in Thymeleaf
            Map<String, Object> learningPathMap = objectMapper.convertValue(coursePath, Map.class);
            model.addAttribute("learningPath", learningPathMap);
            model.addAttribute("courseId", courseId);

            return "course-navigator";
            
        } catch (Exception e) {
            logger.error("Error loading course structure: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load course structure: " + e.getMessage());
            model.addAttribute("courseId", courseId);
            model.addAttribute("userId", user.getId().toString());
            return "course-navigator";
        }
    }

    @GetMapping("/course/details/{courseId}")
    public String viewCourse(@PathVariable String courseId, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Read the user's course suggestions file
            ObjectMapper objectMapper = new ObjectMapper();
            String userId = user.getId().toString();
            Resource suggestionsResource = resourceLoader.getResource(String.format("classpath:static/data/data/suggest_courses_%s.json", userId));
            
            if (!suggestionsResource.exists()) {
                throw new Exception("Course suggestions file not found");
            }

            JsonNode rootNode = objectMapper.readTree(suggestionsResource.getInputStream());
            JsonNode recommendationsNode = rootNode.path("recommendations");
            
            // Find the specific course
            Course course = null;
            for (JsonNode courseNode : recommendationsNode) {
                if (courseNode.path("id").asText().equals(courseId)) {
                    course = new Course();
                    course.setId(courseNode.path("id").asLong());
                    course.setTitle(courseNode.path("title").asText());
                    course.setDescription(courseNode.path("description").asText());
                    course.setConfidenceScore(courseNode.path("confidence_score").asInt());
                    break;
                }
            }

            if (course == null) {
                throw new Exception("Course not found");
            }

            model.addAttribute("course", course);
            model.addAttribute("user", user);
            
            return "course-details";
            
        } catch (Exception e) {
            logger.error("Error loading course {}: {}", courseId, e.getMessage());
            return "redirect:/courses?error=" + e.getMessage();
        }
    }

    @GetMapping("/api/courses/suggestions/{userId}")
    @ResponseBody
    public ResponseEntity<?> getCourseSuggestions(@PathVariable Long userId) {
        try {
            // First, try classpath resource
            String resourcePath = String.format("classpath:static/data/data/suggest_courses_%d.json", userId);
            logger.debug("Attempting to load resource: {}", resourcePath);
            
            Resource resource = resourceLoader.getResource(resourcePath);
            
            // If not found in classpath, try alternate path
            if (!resource.exists()) {
                logger.warn("Resource not found: {}", resourcePath);
                resourcePath = String.format("classpath:data/data/suggest_courses_%d.json", userId);
                resource = resourceLoader.getResource(resourcePath);
            }
            
            // If still not found, try relative path from classpath
            if (!resource.exists()) {
                logger.warn("Resource not found in classpath: {}", resourcePath);
                
                // Try to load from a relative path that would work across different environments
                resourcePath = String.format("file:./src/main/resources/static/data/data/suggest_courses_%d.json", userId);
                resource = resourceLoader.getResource(resourcePath);
                
                if (!resource.exists()) {
                    logger.warn("Resource not found at relative path: {}", resourcePath);
                    return ResponseEntity.notFound().build();
                }
            }

            logger.debug("Resource found at: {}", resource.getURI());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode data = mapper.readTree(resource.getInputStream());
            
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("Error loading course suggestions for user {}: {}", userId, e.getMessage());
            logger.error("Stack trace:", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to load course suggestions"));
        }
    }

    @GetMapping("/course/view")
    public String viewUserCourses(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByEmail(principal.getName());
        if (user == null) {
            return "redirect:/login";
        }

        try {
            // Get selected course IDs
            List<String> selectedCourseIds = new ArrayList<>();
            if (user.getCourseSelected1() != null) selectedCourseIds.add(user.getCourseSelected1());
            if (user.getCourseSelected2() != null) selectedCourseIds.add(user.getCourseSelected2());
            if (user.getCourseSelected3() != null) selectedCourseIds.add(user.getCourseSelected3());

            if (selectedCourseIds.isEmpty()) {
                model.addAttribute("message", "You haven't selected any courses yet.");
                return "courses";
            }

            // Read the user's course suggestions file
            ObjectMapper objectMapper = new ObjectMapper();
            String userId = user.getId().toString();
            Resource suggestionsResource = resourceLoader.getResource(String.format("classpath:static/data/data/suggest_courses_%s.json", userId));
            
            if (!suggestionsResource.exists()) {
                model.addAttribute("error", "Course suggestions file not found");
                return "courses";
            }

            JsonNode rootNode = objectMapper.readTree(suggestionsResource.getInputStream());
            JsonNode recommendationsNode = rootNode.path("recommendations");
            
            List<Course> courses = new ArrayList<>();
            for (JsonNode courseNode : recommendationsNode) {
                String courseId = courseNode.path("id").asText();
                // Only add course if it's in the selected courses list
                if (selectedCourseIds.contains(courseId)) {
                    Course course = new Course();
                    course.setId(courseNode.path("id").asLong());
                    course.setTitle(courseNode.path("title").asText());
                    course.setDescription(courseNode.path("description").asText());
                    course.setConfidenceScore(courseNode.path("confidence_score").asInt());
                    
                    // Add additional fields if available
                    if (courseNode.has("imageUrl")) {
                        course.setImageUrl(courseNode.path("imageUrl").asText());
                    } else {
                        course.setImageUrl("/img/placeholder.png"); // Default image
                    }
                    
                    if (courseNode.has("duration")) {
                        course.setDuration(courseNode.path("duration").asText());
                    }
                    
                    if (courseNode.has("level")) {
                        course.setLevel(courseNode.path("level").asText());
                    }

                    // Add progress information
                    if (courseId.equals(user.getCourseSelected1())) {
                        course.setProgress(user.getCourse1Progress());
                    } else if (courseId.equals(user.getCourseSelected2())) {
                        course.setProgress(user.getCourse2Progress());
                    } else if (courseId.equals(user.getCourseSelected3())) {
                        course.setProgress(user.getCourse3Progress());
                    }
                    
                    courses.add(course);
                }
            }
            
            model.addAttribute("courses", courses);
            model.addAttribute("user", user);
            return "courses";
            
        } catch (Exception e) {
            logger.error("Error loading courses for user {}: {}", user.getId(), e.getMessage());
            model.addAttribute("error", "Error loading course data: " + e.getMessage());
            return "courses";
        }
    }

    @PostMapping("/submit-feedback")
    @ResponseBody
    public ResponseEntity<?> submitFeedback(@RequestBody Map<String, Object> payload, Principal principal) {
        try {
            Long courseId = Long.parseLong(payload.get("courseId").toString());
            Integer rating = Integer.parseInt(payload.get("rating").toString());
            String feedbackText = payload.get("feedbackText").toString();
            
            // Get current user's ID
            User currentUser = userService.findByEmail(principal.getName());
            
            // Submit feedback
            feedbackService.submitFeedback(currentUser.getId(), courseId, rating, feedbackText);
            
            return ResponseEntity.ok().body(Map.of("message", "Feedback submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check-feedback-eligibility")
    @ResponseBody
    public ResponseEntity<?> checkFeedbackEligibility(@RequestParam Long courseId, Principal principal) {
        try {
            // Get current user's ID
            User currentUser = userService.findByEmail(principal.getName());
            
            // Check if feedback already exists
            boolean hasFeedback = feedbackService.hasFeedbackForCourse(currentUser.getId(), courseId);
            
            return ResponseEntity.ok().body(Map.of("canSubmitFeedback", !hasFeedback));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}