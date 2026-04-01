package com.example.demo.service;

import com.example.demo.model.Feedback;
import com.example.demo.repository.FeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {
    
    @Autowired
    private FeedbackRepository feedbackRepository;

    @Transactional
    public Feedback submitFeedback(Long userId, Long courseId, Integer rating, String feedbackText) {
        // Check if feedback already exists
        if (feedbackRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new RuntimeException("Feedback already submitted for this course");
        }

        // Create new feedback
        Feedback feedback = new Feedback(userId, courseId, rating, feedbackText);
        return feedbackRepository.save(feedback);
    }

    public boolean hasFeedbackForCourse(Long userId, Long courseId) {
        return feedbackRepository.existsByUserIdAndCourseId(userId, courseId);
    }
}
