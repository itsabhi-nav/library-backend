package com.library.library_backend.repository;

import com.library.library_backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMemberId(String memberId);

    @Query("SELECT u FROM User u WHERE REPLACE(UPPER(u.memberId), '-', '') = :normalized")
    Optional<User> findByMemberIdNormalized(@Param("normalized") String normalized);

    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByMemberId(String memberId);
    List<User> findAllByRole(User.Role role);
    List<User> findAllByOrderByCreatedAtDesc();
    boolean existsByAssignedSeatId(Long seatId);

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u
        WHERE u.assignedSeat.id = :seatId AND u.role = 'MEMBER' AND u.isActive = true
          AND (:excludeUserId IS NULL OR u.id <> :excludeUserId)
        """)
    boolean isSeatTakenByAnotherActiveMember(@Param("seatId") Long seatId, @Param("excludeUserId") Long excludeUserId);

    @Query("""
        SELECT u FROM User u
        WHERE u.role = 'MEMBER'
          AND (:search IS NULL OR :search = '' OR
               LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.memberId) LIKE LOWER(CONCAT('%', :search, '%')) OR
               u.phoneNumber LIKE CONCAT('%', :search, '%'))
          AND (:status = 'all' OR (:status = 'active' AND u.isActive = true) OR (:status = 'inactive' AND u.isActive = false))
        """)
    Page<User> searchStudents(@Param("search") String search, @Param("status") String status, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'MEMBER' AND u.isActive = true")
    long countActiveMembers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'MEMBER'")
    long countAllMembers();

    @Query("SELECT MAX(CAST(SUBSTRING(u.memberId, LENGTH(u.memberId) - 2, 3) AS int)) FROM User u WHERE u.memberId LIKE :prefix%")
    Integer findMaxSequenceForPrefix(@Param("prefix") String prefix);
}
