package com.vrsec.backend.repository;

import com.vrsec.backend.model.TopicPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicPerformanceRepository extends JpaRepository<TopicPerformance, Long> {
    List<TopicPerformance> findByUserId(Long userId);
    List<TopicPerformance> findByUserIdAndStatus(Long userId, String status);
    Optional<TopicPerformance> findByUserIdAndCourseTitleAndConceptTag(Long userId, String courseTitle, String conceptTag);
}
