package com.vrsec.backend.repository;

import com.vrsec.backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByUserEmailIgnoreCaseAndDeletedFalse(String userEmail);
    List<Course> findByUserEmailIgnoreCase(String userEmail);
    List<Course> findByUserEmailIn(List<String> emails);
}
