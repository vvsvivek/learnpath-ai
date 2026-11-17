package com.vrsec.backend.repository;

import com.vrsec.backend.model.ApprovedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ApprovedCourseRepository extends JpaRepository<ApprovedCourse, Long> {
    List<ApprovedCourse> findByActiveTrue();
    List<ApprovedCourse> findByActiveTrueOrderByTitleAsc();
    Optional<ApprovedCourse> findByIdAndActiveTrue(Long id);
    Optional<ApprovedCourse> findByCodeIgnoreCaseAndActiveTrue(String code);
    Optional<ApprovedCourse> findByTitleIgnoreCaseAndActiveTrue(String title);
    Optional<ApprovedCourse> findByTitleIgnoreCase(String title);
    boolean existsByTitleIgnoreCaseAndActiveTrue(String title);
}
