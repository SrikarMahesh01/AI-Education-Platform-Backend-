package com.example.demo.web;

import com.example.demo.model.User;
import com.example.demo.service.ProfilePictureService;
import com.example.demo.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.Principal;
import java.util.Base64;

@Controller
@RequestMapping("/view")
public class ContentController {
    private static final Logger logger = LoggerFactory.getLogger(ContentController.class);
    private final ResourceLoader resourceLoader;
    private final UserService userService;
    private final ProfilePictureService profilePictureService;

    public ContentController(ResourceLoader resourceLoader,
                             UserService userService,
                             ProfilePictureService profilePictureService) {
        this.resourceLoader = resourceLoader;
        this.userService = userService;
        this.profilePictureService = profilePictureService;
    }

    @GetMapping("/content/{userId}/{courseId}/{activityId}")
    public String showContent(
            @PathVariable String userId,
            @PathVariable String courseId,
            @PathVariable String activityId,
            Principal principal,
            Model model) {
        
        logger.debug("Received request for content - userId: {}, courseId: {}, activityId: {}", 
                userId, courseId, activityId);
        
        try {
            if (principal != null) {
                User user = userService.findByEmail(principal.getName());
                if (user != null) {
                    model.addAttribute("user", user);
                    profilePictureService.getProfilePicture(user.getId()).ifPresent(profilePicture -> {
                        String base64Picture = Base64.getEncoder().encodeToString(profilePicture.getPictureData());
                        model.addAttribute("base64ProfilePicture", base64Picture);
                        model.addAttribute("profilePictureType", profilePicture.getContentType());
                    });
                }
            }

            // First try the static/data/data path
            String resourcePath = String.format("classpath:static/data/data/learning_paths/content/%s/%s/%s/content.json",
                    userId, courseId, activityId);
            Resource resource = resourceLoader.getResource(resourcePath);
            
            // If not found, try to fetch from localhost
            if (!resource.exists()) {
                // Prepare the directory for saving
                File contentDir = new File("src/main/resources/static/data/data/learning_paths/content/" + 
                        userId + "/" + courseId + "/" + activityId);
                contentDir.mkdirs();
                
                // Construct the URL for fetching content
                String contentUrl = String.format("http://localhost:8000/course_content/%s/%s/%s", 
                        userId, courseId, activityId);
                
                // Prepare the file path for saving
                File contentFile = new File(contentDir, "content.json");
                
                try {
                    downloadContentToFile(contentUrl, contentFile);
                    logger.info("Successfully downloaded content for activity: {}", activityId);
                } catch (FileNotFoundException notFound) {
                    logger.warn("Content not found for activity {}. Attempting to generate learning path first.", activityId);
                    String pathUrl = String.format("http://localhost:8000/learning_path/%s/%s", userId, courseId);
                    try (InputStream ignored = new URL(pathUrl).openStream()) {
                        logger.info("Learning path generated for user {}, course {}", userId, courseId);
                    }
                    downloadContentToFile(contentUrl, contentFile);
                    logger.info("Successfully downloaded content after path generation for activity: {}", activityId);
                } catch (Exception e) {
                    logger.error("Failed to fetch content from localhost for activity: " + activityId, e);
                    model.addAttribute("errorTitle", "Course Content Unavailable");
                    model.addAttribute("errorMessage", "This course content is not available right now.");
                    model.addAttribute("error", "Unable to retrieve content. Please try again later.");
                    return "error";
                }

                // Reload the resource
                resource = resourceLoader.getResource("file:" + contentFile.getAbsolutePath());
            }
            
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            logger.info("Successfully loaded content for activity: {}", activityId);
            
            // Add attributes to the model
            model.addAttribute("contentPath", String.format("%s/%s/%s", userId, courseId, activityId));
            model.addAttribute("content", content);
            model.addAttribute("activityId", activityId);
            model.addAttribute("rawJsonUrl", String.format("/data/data/learning_paths/content/%s/%s/%s/content.json", 
                    userId, courseId, activityId));
            
            return "content";
            
        } catch (IOException e) {
            logger.error("Error reading content for activity: " + activityId, e);
            model.addAttribute("errorTitle", "Unable to Load Course Content");
            model.addAttribute("errorMessage", "We couldn't display this activity content right now. Please try again in a moment.");
            model.addAttribute("error", "There was an error loading this content. Please try again later.");
            return "error";
        }
    }

    private void downloadContentToFile(String contentUrl, File contentFile) throws IOException {
        URL url = new URL(contentUrl);
        try (
            InputStream inputStream = url.openStream();
            ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);
            FileOutputStream fileOutputStream = new FileOutputStream(contentFile)
        ) {
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }
}
