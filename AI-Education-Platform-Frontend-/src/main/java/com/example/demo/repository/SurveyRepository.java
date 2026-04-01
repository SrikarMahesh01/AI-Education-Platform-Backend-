package com.example.demo.repository;

import com.example.demo.model.SurveyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyRepository extends JpaRepository<SurveyResponse, Long> {
    SurveyResponse findByUserId(Long userId);
}