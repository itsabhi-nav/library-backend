package com.library.library_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Auto-generated unique member ID, e.g. BR001. Used for login. */
    @Column(name = "member_id", unique = true)
    private String memberId;

    @Email
    @JsonIgnore
    @Column(unique = true)
    private String email;

    @JsonIgnore
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "dob")
    private LocalDate dob;

    /** Consent for WhatsApp messages; default true when member registers. */
    @Builder.Default
    @Column(name = "whatsapp_consent", nullable = false)
    private Boolean whatsappConsent = true;

    /** Admin can deactivate a member; deactivated members cannot login. */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Timestamp of the last successful login. */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    /** Assigned seat for this member (set at registration). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_seat_id")
    private Seat assignedSeat;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public enum Role {
        ADMIN,
        LIBRARIAN,
        MEMBER
    }
}
