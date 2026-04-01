package com.example.demo.dto;

public class CourseRecommendation {
    private Long id;
    private String title;
    private String description;
    private double confidenceScore;

    public CourseRecommendation(Long id, String title, String description, double confidenceScore) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.confidenceScore = confidenceScore;
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

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
