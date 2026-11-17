package com.vrsec.backend.repository;

import com.vrsec.backend.model.SuggestedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuggestedCourseRepository extends JpaRepository<SuggestedCourse, Long> {
    List<SuggestedCourse> findByFacultyEmail(String facultyEmail);
    List<SuggestedCourse> findByTargetBranchIgnoreCase(String targetBranch);
}
