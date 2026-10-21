package com.vrsec.backend.repository;

import com.vrsec.backend.model.SuggestedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestedCourseRepository extends JpaRepository<SuggestedCourse, Long> {
}