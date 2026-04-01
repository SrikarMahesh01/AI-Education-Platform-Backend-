package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_progress")
public class CourseProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "course_id")
    private String courseId;

    @Column(name = "total_activities")
    private Integer totalActivities;

    @Column(name = "completed_activities")
    private Integer completedActivities;

    @Column(name = "progress_percentage")
    private Integer progressPercentage;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Default constructor
    public CourseProgress() {
        this.totalActivities = 0;
        this.completedActivities = 0;
        this.progressPercentage = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    // Constructor with parameters
    public CourseProgress(Long userId, String courseId) {
        this.userId = userId;
        this.courseId = courseId;
        this.totalActivities = 0;
        this.completedActivities = 0;
        this.progressPercentage = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and setters
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

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public Integer getTotalActivities() {
        return totalActivities != null ? totalActivities : 0;
    }

    public void setTotalActivities(Integer totalActivities) {
        this.totalActivities = totalActivities != null ? totalActivities : 0;
    }

    public Integer getCompletedActivities() {
        return completedActivities != null ? completedActivities : 0;
    }

    public void setCompletedActivities(Integer completedActivities) {
        this.completedActivities = completedActivities != null ? completedActivities : 0;
    }

    public Integer getProgressPercentage() {
        return progressPercentage != null ? progressPercentage : 0;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage != null ? progressPercentage : 0;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
