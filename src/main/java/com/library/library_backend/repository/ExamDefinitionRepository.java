package com.library.library_backend.repository;

import com.library.library_backend.model.ExamDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamDefinitionRepository extends JpaRepository<ExamDefinition, Long> {

    List<ExamDefinition> findAllByIsActiveTrueOrderBySortOrderAsc();

    Optional<ExamDefinition> findByCode(String code);
}
