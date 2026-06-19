package com.library.library_backend.service;

import com.library.library_backend.dto.BookingRequest;
import com.library.library_backend.dto.ShiftRequest;
import com.library.library_backend.model.*;
import org.springframework.transaction.annotation.Transactional;
import com.library.library_backend.repository.BookingRepository;
import com.library.library_backend.repository.SeatRepository;
import com.library.library_backend.repository.ShiftRepository;
import com.library.library_backend.repository.SubscriptionRepository;
import com.library.library_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class SeatBookingService {

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Seat> getAllSeats() {
        return seatRepository.findAll();
    }

    public Seat addSeat(Seat seat) {
        return seatRepository.save(seat);
    }

    public Seat updateSeatStatus(Long seatId, Seat.SeatStatus status) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));
        seat.setStatus(status);
        return seatRepository.save(seat);
    }

    public List<Shift> getAllShifts() {
        return shiftRepository.findAll();
    }

    public Shift addShift(Shift shift) {
        return shiftRepository.save(shift);
    }

    public List<Booking> getBookingsByDate(LocalDate date) {
        return bookingRepository.findByBookingDate(date);
    }

    public Booking createBooking(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check active subscription
        Subscription activeSub = subscriptionRepository.findActiveSubscriptionForUser(userId, request.getBookingDate())
                .orElseThrow(() -> new IllegalStateException("No active membership found for date: " + request.getBookingDate()));

        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));

        if (seat.getStatus() == Seat.SeatStatus.MAINTENANCE) {
            throw new IllegalStateException("Seat is currently under maintenance");
        }

        Shift shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new IllegalArgumentException("Shift not found"));

        // Prevent double booking of the same seat + shift + date
        Optional<Booking> conflicts = bookingRepository.findActiveBookingBySeatShiftAndDate(
                seat.getId(), shift.getId(), request.getBookingDate()
        );
        if (conflicts.isPresent()) {
            throw new IllegalStateException("Seat is already booked for this shift and date");
        }

        // Prevent the user from booking the same shift + date twice
        List<Booking> userBookingsToday = bookingRepository.findActiveBookingsForUserOnDate(userId, request.getBookingDate());
        boolean alreadyBookedShift = userBookingsToday.stream()
                .anyMatch(b -> b.getShift().getId().equals(shift.getId()));
        if (alreadyBookedShift) {
            throw new IllegalStateException("You already have a booking for this shift on this date");
        }

        Booking booking = Booking.builder()
                .user(user)
                .seat(seat)
                .shift(shift)
                .subscription(activeSub)
                .bookingDate(request.getBookingDate())
                .status(Booking.BookingStatus.ACTIVE)
                .build();

        return bookingRepository.save(booking);
    }

    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Ensure user owns this booking
        if (!booking.getUser().getId().equals(userId)) {
            throw new SecurityException("Unauthorized action");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<Seat> getAssignableSeats(Long excludeUserId) {
        return seatRepository.findAll().stream()
                .filter(s -> s.getStatus() != Seat.SeatStatus.MAINTENANCE)
                .filter(s -> !userRepository.isSeatTakenByAnotherActiveMember(s.getId(), excludeUserId))
                .toList();
    }

    @Transactional
    public Shift updateShift(Long id, ShiftRequest request) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found"));
        shift.setName(request.getName());
        shift.setStartTime(java.time.LocalTime.parse(request.getStartTime()));
        shift.setEndTime(java.time.LocalTime.parse(request.getEndTime()));
        return shiftRepository.save(shift);
    }

    @Transactional
    public void deleteShift(Long id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found"));
        shift.setIsActive(false);
        shiftRepository.save(shift);
    }

    @Transactional
    public void bulkSetCapacity(int total) {
        if (total < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        List<Seat> seats = seatRepository.findAll();
        int currentCount = seats.size();

        if (currentCount < total) {
            int maxNum = 0;
            for (Seat s : seats) {
                try {
                    String numPart = s.getSeatNumber().replaceAll("[^0-9]", "");
                    if (!numPart.isEmpty()) {
                        int num = Integer.parseInt(numPart);
                        if (num > maxNum) {
                            maxNum = num;
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }

            for (int i = currentCount + 1; i <= total; i++) {
                maxNum++;
                Seat newSeat = Seat.builder()
                        .seatNumber("Seat-" + String.format("%02d", maxNum))
                        .status(Seat.SeatStatus.AVAILABLE)
                        .hasPowerOutlet(true)
                        .build();
                seatRepository.save(newSeat);
            }
        } else if (currentCount > total) {
            seats.sort((s1, s2) -> {
                try {
                    int n1 = Integer.parseInt(s1.getSeatNumber().replaceAll("[^0-9]", ""));
                    int n2 = Integer.parseInt(s2.getSeatNumber().replaceAll("[^0-9]", ""));
                    return Integer.compare(n2, n1);
                } catch (NumberFormatException e) {
                    return s2.getSeatNumber().compareTo(s1.getSeatNumber());
                }
            });

            int toRemove = currentCount - total;
            for (int i = 0; i < toRemove; i++) {
                Seat seat = seats.get(i);
                if (userRepository.existsByAssignedSeatId(seat.getId()) || bookingRepository.existsBySeatId(seat.getId())) {
                    throw new IllegalStateException("Cannot reduce seat capacity because " + seat.getSeatNumber() + " is currently assigned to a user or has booking history.");
                }
                seatRepository.delete(seat);
            }
        }
    }
}
