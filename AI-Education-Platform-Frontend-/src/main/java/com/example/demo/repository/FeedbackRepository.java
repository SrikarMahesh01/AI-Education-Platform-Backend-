package com.example.demo.repository;

import com.example.demo.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // Check if feedback exists for a specific user and course
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    // Find feedback by user and course
    Optional<Feedback> findByUserIdAndCourseId(Long userId, Long courseId);
}
