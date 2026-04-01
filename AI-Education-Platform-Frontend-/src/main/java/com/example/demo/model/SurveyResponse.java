package com.example.demo.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "survey_responses")
public class SurveyResponse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;
    
    private String professionalStatus;
    private String learningMotivation;
    private String timeAvailability;
    private String learningExperience;
    private String preferredLearningFormat;
    private String techComfortLevel;
    private String careerField;
    private String learningChallenges;
    private String onlineLearningExperience;
    private String skillDevelopmentGoal;
    
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

    public String getProfessionalStatus() {
        return professionalStatus;
    }

    public void setProfessionalStatus(String professionalStatus) {
        this.professionalStatus = professionalStatus;
    }

    public String getLearningMotivation() {
        return learningMotivation;
    }

    public void setLearningMotivation(String learningMotivation) {
        this.learningMotivation = learningMotivation;
    }

    public String getTimeAvailability() {
        return timeAvailability;
    }

    public void setTimeAvailability(String timeAvailability) {
        this.timeAvailability = timeAvailability;
    }

    public String getLearningExperience() {
        return learningExperience;
    }

    public void setLearningExperience(String learningExperience) {
        this.learningExperience = learningExperience;
    }

    public String getPreferredLearningFormat() {
        return preferredLearningFormat;
    }

    public void setPreferredLearningFormat(String preferredLearningFormat) {
        this.preferredLearningFormat = preferredLearningFormat;
    }

    public String getTechComfortLevel() {
        return techComfortLevel;
    }

    public void setTechComfortLevel(String techComfortLevel) {
        this.techComfortLevel = techComfortLevel;
    }

    public String getCareerField() {
        return careerField;
    }

    public void setCareerField(String careerField) {
        this.careerField = careerField;
    }

    public String getLearningChallenges() {
        return learningChallenges;
    }

    public void setLearningChallenges(String learningChallenges) {
        this.learningChallenges = learningChallenges;
    }

    public String getOnlineLearningExperience() {
        return onlineLearningExperience;
    }

    public void setOnlineLearningExperience(String onlineLearningExperience) {
        this.onlineLearningExperience = onlineLearningExperience;
    }

    public String getSkillDevelopmentGoal() {
        return skillDevelopmentGoal;
    }

    public void setSkillDevelopmentGoal(String skillDevelopmentGoal) {
        this.skillDevelopmentGoal = skillDevelopmentGoal;
    }
}
