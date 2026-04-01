package com.example.demo.dto;

public class QuizAnswerExportDTO {
    private Long userId;
    private int questionNumber;
    private String selectedOption;
    private boolean isCorrect;

    public QuizAnswerExportDTO(Long userId, int questionNumber, String selectedOption, boolean isCorrect) {
        this.userId = userId;
        this.questionNumber = questionNumber;
        this.selectedOption = selectedOption;
        this.isCorrect = isCorrect;
    }

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(int questionNumber) {
        this.questionNumber = questionNumber;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public void setSelectedOption(String selectedOption) {
        this.selectedOption = selectedOption;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }
}
