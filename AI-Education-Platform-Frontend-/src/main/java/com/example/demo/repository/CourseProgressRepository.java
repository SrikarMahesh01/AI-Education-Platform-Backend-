package com.example.demo.repository;

import com.example.demo.model.CourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {
    Optional<CourseProgress> findByUserIdAndCourseId(Long userId, String courseId);
    List<CourseProgress> findByUserId(Long userId);
    void deleteByUserIdAndCourseId(Long userId, String courseId);
}
