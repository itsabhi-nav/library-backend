package com.library.library_backend.repository;

import com.library.library_backend.model.Locker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LockerRepository extends JpaRepository<Locker, Long> {
    List<Locker> findByStatus(Locker.LockerStatus status);
}
