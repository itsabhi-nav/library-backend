package com.library.library_backend.repository;

import com.library.library_backend.model.UserExamTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserExamTargetRepository extends JpaRepository<UserExamTarget, Long> {

    Optional<UserExamTarget> findByUserId(Long userId);
}
