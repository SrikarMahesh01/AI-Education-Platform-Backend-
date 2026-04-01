package com.example.demo.repository;

import com.example.demo.model.QuizResult;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findByUserOrderBySubmissionTimeDesc(User user);
    List<QuizResult> findTop5ByOrderByPercentageDesc();
}
