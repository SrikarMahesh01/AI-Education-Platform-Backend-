package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;

@Entity
@Table(name = "feedback", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id"}))
public class Feedback implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "feedback_text", length = 1000)
    private String feedbackText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Feedback() {
        this.createdAt = LocalDateTime.now();
    }

    public Feedback(Long userId, Long courseId, Integer rating, String feedbackText) {
        this.userId = userId;
        this.courseId = courseId;
        this.rating = rating;
        this.feedbackText = feedbackText;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Add toString for debugging
    @Override
    public String toString() {
        return "Feedback{" +
               "id=" + id +
               ", userId=" + userId +
               ", courseId=" + courseId +
               ", rating=" + rating +
               ", feedbackText='" + feedbackText + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
}
