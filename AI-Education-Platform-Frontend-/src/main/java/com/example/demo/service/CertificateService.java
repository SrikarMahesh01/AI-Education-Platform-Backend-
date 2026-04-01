package com.example.demo.service;

import com.example.demo.model.CourseProgress;
import com.example.demo.model.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);

    @Autowired
    private UserService userService;

    @Autowired
    private CourseProgressService courseProgressService;

    @Autowired
    private CourseService courseService;

    private PDFont loadFont(String fontName) throws IOException {
        try (InputStream fontStream = getClass().getResourceAsStream("/fonts/" + fontName)) {
            return PDType0Font.load(new PDDocument(), fontStream);
        }
    }

    public byte[] generateCertificate(Long userId, String courseId) throws IOException {
        // Get user and course progress
        User user = userService.findById(userId);
        CourseProgress progress = courseProgressService.findByUserIdAndCourseId(userId, courseId);

        // Verify course completion
        if (progress.getProgressPercentage() < 100) {
            throw new IllegalStateException("Course not completed yet");
        }

        // Get course title with detailed logging
        logger.debug("Attempting to get course title for courseId: {}", courseId);
        String courseTitle = courseService.getCourseTitle(courseId)
            .orElseGet(() -> {
                logger.warn("Fallback to generic title for course ID: {}", courseId);
                return "Course " + courseId;
            });
        
        logger.info("Resolved course title for certificate: {} for ID: {}", courseTitle, courseId);

        return generateCertificate(user, courseTitle);
    }

    public byte[] generateCertificate(User user, String courseTitle) throws IOException {
        // Load the certificate template
        try (InputStream templateStream = getClass().getResourceAsStream("/certificate.pdf")) {
            PDDocument document = PDDocument.load(templateStream);
            PDPage page = document.getPage(0);

            // Load custom fonts
            PDFont consolasBold = loadFont("CONSOLAB.TTF");
            PDFont consolas = loadFont("CONSOLA.TTF");

            PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
            
            // Write the name
            contentStream.setFont(consolasBold, 24);
            String fullName = user.getFirstName() + " " + user.getLastName();
            float nameWidth = consolasBold.getStringWidth(fullName) / 1000 * 24;
            contentStream.beginText();
            contentStream.newLineAtOffset((page.getMediaBox().getWidth() - nameWidth) / 2, 310);
            contentStream.showText(fullName);
            contentStream.endText();

            // Write the course title
            contentStream.setFont(consolas, 28);
            float courseWidth = consolas.getStringWidth(courseTitle) / 1000 * 28;
            contentStream.beginText();
            contentStream.newLineAtOffset((page.getMediaBox().getWidth() - courseWidth) / 2, 210);
            contentStream.showText(courseTitle);
            contentStream.endText();

            // Write the date
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            contentStream.setFont(consolas, 16);
            float dateWidth = consolas.getStringWidth(date) / 1000 * 16;
            contentStream.beginText();
            contentStream.newLineAtOffset((page.getMediaBox().getWidth() - dateWidth) / 2, 20);
            contentStream.showText(date);
            contentStream.endText();

            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            document.close();

            return baos.toByteArray();
        }
    }
}
