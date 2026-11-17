package com.vrsec.backend.repository;

import com.vrsec.backend.model.CourseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRequestRepository extends JpaRepository<CourseRequest, Long> {
    List<CourseRequest> findByStudentEmailIgnoreCaseOrderByCreatedAtDesc(String studentEmail);
    List<CourseRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<CourseRequest> findAllByOrderByCreatedAtDesc();
}
