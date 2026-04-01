package com.example.demo.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_progress")
public class ActivityProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String courseName;

    @Column(nullable = false)
    private Integer moduleIndex;

    @Column(nullable = false)
    private String activityName;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    public ActivityProgress() {
    }

    public ActivityProgress(User user, String courseName, Integer moduleIndex, String activityName) {
        this.user = user;
        this.courseName = courseName;
        this.moduleIndex = moduleIndex;
        this.activityName = activityName;
        this.completed = false;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getModuleIndex() {
        return moduleIndex;
    }

    public void setModuleIndex(Integer moduleIndex) {
        this.moduleIndex = moduleIndex;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
