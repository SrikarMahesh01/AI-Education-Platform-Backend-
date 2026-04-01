package com.example.demo.dto;

import java.util.List;

public class UserQuizAnswersDTO {
    private Long userId;
    private List<QuizAnswerDTO> answers;

    public UserQuizAnswersDTO(Long userId, List<QuizAnswerDTO> answers) {
        this.userId = userId;
        this.answers = answers;
    }

    // Getters and setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<QuizAnswerDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuizAnswerDTO> answers) {
        this.answers = answers;
    }
}
