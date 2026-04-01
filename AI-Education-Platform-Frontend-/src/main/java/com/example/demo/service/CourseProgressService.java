package com.example.demo.service;

import com.example.demo.model.CourseProgress;
import com.example.demo.repository.CourseProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CourseProgressService {

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    public CourseProgress findByUserIdAndCourseId(Long userId, String courseId) {
        return courseProgressRepository.findByUserIdAndCourseId(userId, courseId)
            .orElseThrow(() -> new IllegalStateException("Course progress not found"));
    }

    public List<CourseProgress> findByUserId(Long userId) {
        return courseProgressRepository.findByUserId(userId);
    }

    public CourseProgress save(CourseProgress courseProgress) {
        return courseProgressRepository.save(courseProgress);
    }
}
