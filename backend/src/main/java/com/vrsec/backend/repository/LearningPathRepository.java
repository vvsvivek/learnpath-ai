package com.vrsec.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vrsec.backend.model.LearningPath;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    List<LearningPath> findByStudentId(Long studentId);
}