package com.vrsec.backend.repository;

import com.vrsec.backend.model.QuizAttempt;
import com.vrsec.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    List<QuizAttempt> findByUser(User user);
}