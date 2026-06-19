package com.library.library_backend.repository;

import com.library.library_backend.model.LibraryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LibraryConfigRepository extends JpaRepository<LibraryConfig, String> {
}
