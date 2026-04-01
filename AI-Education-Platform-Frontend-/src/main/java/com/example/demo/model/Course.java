package com.example.demo.model;

import java.io.Serializable;

public class Course implements Serializable {
    private Long id;
    private String title;
    private String description;
    private Integer confidenceScore;
    private String imageUrl;
    private String duration;
    private String level;
    private Integer progress;

    // Default constructor
    public Course() {
        this.progress = 0; // Initialize progress to 0
    }

    // Parameterized constructor
    public Course(Long id, String title, String description, Integer confidenceScore) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.confidenceScore = confidenceScore;
        this.progress = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Integer confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress != null ? progress : 0;
    }

    // toString method for easy debugging
    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", confidenceScore=" + confidenceScore +
                ", imageUrl='" + imageUrl + '\'' +
                ", duration='" + duration + '\'' +
                ", level='" + level + '\'' +
                ", progress=" + progress +
                '}';
    }
}
