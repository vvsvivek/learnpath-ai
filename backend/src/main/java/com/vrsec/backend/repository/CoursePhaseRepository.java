package com.vrsec.backend.repository;

import com.vrsec.backend.model.CoursePhase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CoursePhaseRepository extends JpaRepository<CoursePhase, Long> {
    List<CoursePhase> findByCourseIdOrderByPhaseNumberAsc(Long courseId);
}