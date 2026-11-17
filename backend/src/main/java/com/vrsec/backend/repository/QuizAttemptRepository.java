package com.vrsec.backend.repository;

import com.vrsec.backend.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUserId(Long userId);
    List<QuizAttempt> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<QuizAttempt> findByCourseId(Long courseId);
    List<QuizAttempt> findByPhaseId(Long phaseId);
}
