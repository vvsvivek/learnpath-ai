package com.vrsec.backend.repository;

import com.vrsec.backend.model.CoursePhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoursePhaseRepository extends JpaRepository<CoursePhase, Long> {
    List<CoursePhase> findByCourseIdOrderByPhaseNumberAsc(Long courseId);
    Optional<CoursePhase> findByCourseIdAndPhaseNumber(Long courseId, Integer phaseNumber);
}
