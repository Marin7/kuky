package com.kuky.backend.scheduling.service;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/** Polls for confirmed bookings crossing the 24h-before mark and sends reminder emails. */
@Component
public class BookingReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingReminderScheduler.class);

    private final Clock clock;
    private final BookingRepository bookingRepository;
    private final BookingEmailService emailService;
    private final SchedulingProperties props;

    public BookingReminderScheduler(Clock clock, BookingRepository bookingRepository,
                                    BookingEmailService emailService, SchedulingProperties props) {
        this.clock = clock;
        this.bookingRepository = bookingRepository;
        this.emailService = emailService;
        this.props = props;
    }

    @Scheduled(fixedDelay = 900_000)
    public void sendDueReminders() {
        Instant now = clock.instant();
        List<BookingRepository.ReminderDueView> due = bookingRepository.findBookingsDueForReminder(now);

        for (BookingRepository.ReminderDueView booking : due) {
            if (!bookingRepository.claimReminder(booking.id(), now)) {
                continue;
            }
            emailService.sendReminderToStudent(booking.email(), booking.slotStart(), booking.zoomJoinUrl());
            if (booking.companionStudentEmail() != null) {
                emailService.sendReminderToStudent(booking.companionStudentEmail(), booking.slotStart(), booking.zoomJoinUrl());
            }
            emailService.sendReminderToTeacher(props.getScheduling().getTeacherEmail(), booking.email(),
                    booking.slotStart(), booking.zoomJoinUrl());
            log.info("Sent 24h reminder for booking {}", booking.id());
        }
    }
}
