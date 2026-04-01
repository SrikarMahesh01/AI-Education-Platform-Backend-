package com.example.demo.aspect;

import com.example.demo.service.DataExportService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DataExportAspect {
    private static final Logger logger = LoggerFactory.getLogger(DataExportAspect.class);

    @Autowired
    private DataExportService dataExportService;

    @AfterReturning("execution(* com.example.demo.repository.QuizResultRepository.save(..))")
    public void afterQuizResultSave() {
        logger.info("Quiz result updated, triggering export");
        dataExportService.exportQuizResults();
    }

    @AfterReturning("execution(* com.example.demo.repository.SurveyRepository.save(..))")
    public void afterSurveyResponseSave() {
        logger.info("Survey response updated, triggering export");
        dataExportService.exportSurveyResponses();
    }
}
