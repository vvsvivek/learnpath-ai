package com.vrsec.backend.repository;

import com.vrsec.backend.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByPhaseIdOrderByIdAsc(Long phaseId);
    List<QuizQuestion> findByCourseId(Long courseId);
}
