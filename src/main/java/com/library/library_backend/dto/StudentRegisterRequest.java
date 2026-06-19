package com.library.library_backend.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * Used by admin/librarian to register a new student.
 * Login credentials: member_id (auto-generated) + password (phone number by default).
 */
@Data
public class StudentRegisterRequest {
    // Required
    private String fullName;
    private String phoneNumber;

    // Optional
    private String address;
    private LocalDate dob;
    /** Defaults to true when omitted — member agrees to library WhatsApp at registration. */
    private Boolean whatsappConsent;
    private Long   planId;
    /** When true, apply {@link #seatId} on update (null clears assigned seat). */
    private Boolean assignSeat;
    private Long   seatId;
    private String paymentMethod; // CASH, UPI, CARD — defaults to CASH
    private String password;      // If blank, defaults to phoneNumber
}
