package com.example.demo.service;

import com.example.demo.model.QuizResult;
import com.example.demo.model.QuizAnswer;
import com.example.demo.model.User;
import com.example.demo.repository.QuizResultRepository;
import com.example.demo.repository.QuizAnswerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuizService {
    
    @Autowired
    private QuizResultRepository quizResultRepository;
    
    @Autowired
    private QuizAnswerRepository quizAnswerRepository;

    @Transactional
    public QuizResult saveQuizResult(User user, int score, int totalQuestions, List<QuizAnswer> answers) {
        QuizResult result = new QuizResult(user, score, totalQuestions);
        result = quizResultRepository.save(result);
        
        // Save each answer with reference to the user
        for (QuizAnswer answer : answers) {
            answer.setUser(user);
            quizAnswerRepository.save(answer);
        }
        
        return result;
    }

    public List<QuizResult> getUserResults(User user) {
        return quizResultRepository.findByUserOrderBySubmissionTimeDesc(user);
    }

    public QuizResult getQuizResult(Long quizId) {
        return quizResultRepository.findById(quizId).orElse(null);
    }

    public List<QuizAnswer> getQuizAnswers(QuizResult quizResult) {
        return quizAnswerRepository.findByUserOrderByQuestionNumber(quizResult.getUser());
    }

    public List<QuizResult> getTopResults() {
        return quizResultRepository.findTop5ByOrderByPercentageDesc();
    }
}
