package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

@Service
public class LearningPathVerificationService {
    private static final Logger logger = LoggerFactory.getLogger(LearningPathVerificationService.class);
    private static final String LOG_FILE_NAME = "learning_path_verification.log";
    private static final String LEARNING_PATHS_DIR = "src/main/resources/static/data/data/learning_paths/";
    private static final String SUGGESTIONS_DIR = "src/main/resources/static/data/data/";

    @Value("${app.log.directory:#{systemProperties['user.dir']}}")
    private String logDirectory;

    private final ObjectMapper objectMapper;

    @Autowired
    public LearningPathVerificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private void logToFile(String message) {
        try {
            // Use provided log directory or default to current working directory
            File logFile = new File(logDirectory, LOG_FILE_NAME);
            
            // Ensure log directory exists
            logFile.getParentFile().mkdirs();

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                writer.println(String.format("[%s] %s", timestamp, message));
            }
        } catch (IOException e) {
            // Fallback logging
            logger.error("Failed to write to log file", e);
        }
    }

    private boolean downloadLearningPath(Long userId, Integer courseId) {
        try {
            // Explicitly use the source resources directory for learning paths
            String basePath = LEARNING_PATHS_DIR;
            File baseDir = new File(basePath);
            
            // Ensure the directory exists
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }

            String pathFilename = String.format("%d_%d_path.json", userId, courseId);
            File pathFile = new File(baseDir, pathFilename);
            
            // Construct URL for learning path
            String urlString = String.format("http://localhost:8000/learning_path/%d/%d", userId, courseId);
            URL url = new URL(urlString);
            
            // Log detailed URL and file information
            logToFile(String.format("Attempting to download learning path from URL: %s", urlString));
            logToFile(String.format("Target save location: %s", pathFile.getAbsolutePath()));
            
            // Start timing the download
            long startTime = System.currentTimeMillis();
            
            // Download file
            try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(pathFile)) {
                
                long bytesTransferred = fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                
                // Calculate download time
                long endTime = System.currentTimeMillis();
                long downloadDuration = endTime - startTime;
                
                logToFile(String.format("Successfully downloaded learning path for user %d, course %d", userId, courseId));
                logToFile(String.format("Download details - Bytes transferred: %d, Duration: %d ms", 
                    bytesTransferred, downloadDuration));
                
                // Verify file size and contents
                if (pathFile.exists() && pathFile.length() > 0) {
                    logToFile(String.format("Verifying downloaded file - Size: %d bytes", pathFile.length()));
                    
                    // Optional: Log first few lines of the file
                    try {
                        List<String> firstLines = Files.lines(pathFile.toPath())
                            .limit(3)
                            .collect(Collectors.toList());
                        logToFile("First few lines of the downloaded file:");
                        firstLines.forEach(line -> logToFile("  " + line));
                    } catch (IOException e) {
                        logToFile("Could not read file contents: " + e.getMessage());
                    }
                }
                
                return true;
            }
        } catch (Exception e) {
            logToFile(String.format("Detailed error downloading learning path for user %d, course %d:", userId, courseId));
            logToFile("Error message: " + e.getMessage());
            logToFile("Error type: " + e.getClass().getName());
            
            // Log stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logToFile("Full error stack trace:");
            logToFile(sw.toString());
            
            logger.error("Error downloading learning path", e);
            return false;
        }
    }

    public boolean verifyAndFetchLearningPaths(Long userId) {
        logToFile(String.format("Starting comprehensive learning path verification for user %d", userId));

        try {
            // Explicitly use the source resources directory for suggestions
            String suggestionsPath = SUGGESTIONS_DIR;
            File suggestionsDir = new File(suggestionsPath);
            
            // Ensure the directory exists
            if (!suggestionsDir.exists()) {
                suggestionsDir.mkdirs();
            }
            
            File suggestionsFile = new File(suggestionsDir, String.format("suggest_courses_%d.json", userId));
            
            if (!suggestionsFile.exists()) {
                logToFile(String.format("Suggestions file not found for user %d", userId));
                return false;
            }

            // Parse suggestions JSON
            Map<String, Object> suggestionsData = objectMapper.readValue(suggestionsFile, Map.class);
            List<Map<String, Object>> recommendations = (List<Map<String, Object>>) suggestionsData.get("recommendations");
            List<Integer> recommendedCourseIds = recommendations.stream()
                .map(rec -> (Integer) rec.get("id"))
                .collect(Collectors.toList());

            return verifyAndFetchLearningPaths(userId, recommendedCourseIds);

        } catch (Exception e) {
            logToFile(String.format("Error during learning path verification for user %d: %s", 
                userId, e.getMessage()));
            logger.error("Error verifying learning paths", e);
            return false;
        }
    }

    public boolean verifyAndFetchLearningPaths(Long userId, List<Integer> courseIds) {
        logToFile(String.format("Starting learning path verification for user %d, selected courses: %s", userId, courseIds));

        if (courseIds == null || courseIds.isEmpty()) {
            logToFile(String.format("No course IDs provided for user %d", userId));
            return false;
        }

        List<Integer> missingPathCourses = new ArrayList<>();

        for (Integer courseId : courseIds) {
            String pathFilename = String.format("%d_%d_path.json", userId, courseId);
            File pathFile = new File(LEARNING_PATHS_DIR, pathFilename);

            boolean exists = pathFile.exists();
            logToFile(String.format("Checking path for user %d, course %d: %s",
                userId, courseId, exists ? "EXISTS" : "NOT FOUND"));

            if (!exists) {
                missingPathCourses.add(courseId);
            }
        }

        if (missingPathCourses.isEmpty()) {
            logToFile(String.format("Learning path verification result for user %d: ALL SELECTED PATHS EXIST", userId));
            return true;
        }

        logToFile(String.format("Starting download of missing learning paths for user %d: %s",
            userId, missingPathCourses));

        boolean allDownloaded = true;
        for (Integer courseId : missingPathCourses) {
            boolean downloaded = downloadLearningPath(userId, courseId);
            if (!downloaded) {
                allDownloaded = false;
            }
        }

        if (allDownloaded) {
            logToFile(String.format("Successfully downloaded all selected learning paths for user %d", userId));
            return true;
        }

        logToFile(String.format("Failed to download all selected learning paths for user %d", userId));
        return false;
    }
}
