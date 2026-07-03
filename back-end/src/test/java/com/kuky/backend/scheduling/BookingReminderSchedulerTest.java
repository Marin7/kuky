package com.kuky.backend.scheduling;

import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.repository.BookingRepository;
import com.kuky.backend.scheduling.service.BookingEmailService;
import com.kuky.backend.scheduling.service.BookingReminderScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Pure-logic tests for the reminder poll loop — no Spring context, no database. */
class BookingReminderSchedulerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T06:00:00Z"), java.time.ZoneOffset.UTC);
    private static final String TEACHER_EMAIL = "paula@kuky.es";

    private BookingRepository bookingRepository;
    private BookingEmailService emailService;
    private BookingReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        emailService = mock(BookingEmailService.class);
        SchedulingProperties props = new SchedulingProperties();
        props.getScheduling().setTeacherEmail(TEACHER_EMAIL);
        scheduler = new BookingReminderScheduler(CLOCK, bookingRepository, emailService, props);
    }

    @Test
    void sendsReminderToStudentAndTeacher_whenBookingDueAndClaimSucceeds() {
        UUID bookingId = UUID.randomUUID();
        Instant slotStart = CLOCK.instant().plusSeconds(23 * 3600);
        String joinUrl = "https://zoom.example/abc";
        BookingRepository.ReminderDueView due =
                new BookingRepository.ReminderDueView(bookingId, "student@kuky.es", slotStart, joinUrl, null);

        when(bookingRepository.findBookingsDueForReminder(CLOCK.instant())).thenReturn(List.of(due));
        when(bookingRepository.claimReminder(eq(bookingId), any())).thenReturn(true);

        scheduler.sendDueReminders();

        verify(emailService).sendReminderToStudent("student@kuky.es", slotStart, joinUrl);
        verify(emailService).sendReminderToTeacher(TEACHER_EMAIL, "student@kuky.es", slotStart, joinUrl);
    }

    @Test
    void sendsReminderToSecondStudentToo_whenBookingIsShared() {
        UUID bookingId = UUID.randomUUID();
        Instant slotStart = CLOCK.instant().plusSeconds(23 * 3600);
        String joinUrl = "https://zoom.example/abc";
        BookingRepository.ReminderDueView due = new BookingRepository.ReminderDueView(
                bookingId, "student@kuky.es", slotStart, joinUrl, "second@kuky.es");

        when(bookingRepository.findBookingsDueForReminder(CLOCK.instant())).thenReturn(List.of(due));
        when(bookingRepository.claimReminder(eq(bookingId), any())).thenReturn(true);

        scheduler.sendDueReminders();

        verify(emailService).sendReminderToStudent("student@kuky.es", slotStart, joinUrl);
        verify(emailService).sendReminderToStudent("second@kuky.es", slotStart, joinUrl);
        verify(emailService).sendReminderToTeacher(TEACHER_EMAIL, "student@kuky.es", slotStart, joinUrl);
    }

    @Test
    void sendsNoEmail_whenClaimFails() {
        UUID bookingId = UUID.randomUUID();
        Instant slotStart = CLOCK.instant().plusSeconds(23 * 3600);
        BookingRepository.ReminderDueView due = new BookingRepository.ReminderDueView(
                bookingId, "student@kuky.es", slotStart, "https://zoom.example/abc", null);

        when(bookingRepository.findBookingsDueForReminder(CLOCK.instant())).thenReturn(List.of(due));
        when(bookingRepository.claimReminder(eq(bookingId), any())).thenReturn(false);

        scheduler.sendDueReminders();

        verifyNoInteractions(emailService);
    }

    @Test
    void sendsNoEmail_whenNothingDue() {
        when(bookingRepository.findBookingsDueForReminder(CLOCK.instant())).thenReturn(List.of());

        scheduler.sendDueReminders();

        verify(bookingRepository, never()).claimReminder(any(), any());
        verifyNoInteractions(emailService);
    }

    @Test
    void sendsNoEmail_whenOnlyBookingWithinWindowWasCancelled() {
        // A cancelled booking is never returned by findBookingsDueForReminder (BookingReminderRepositoryTest
        // verifies this against the real query); the scheduler must stay a no-op when the due-list is empty
        // for that reason, so neither the student nor the teacher is reminded of a class that no longer exists.
        when(bookingRepository.findBookingsDueForReminder(CLOCK.instant())).thenReturn(List.of());

        scheduler.sendDueReminders();

        verify(bookingRepository, never()).claimReminder(any(), any());
        verify(emailService, never()).sendReminderToStudent(any(), any(), any());
        verify(emailService, never()).sendReminderToTeacher(any(), any(), any(), any());
    }
}
