package com.kuky.backend.scheduling;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.scheduling.service.BookingEmailService;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BookingEmailServiceTest {

    private static final Session SESSION = Session.getInstance(new Properties());

    private JavaMailSender mailSender;
    private UserRepository userRepository;
    private BookingEmailService emailService;

    private final UUID bookingId = UUID.randomUUID();
    private final Instant slotStart = Instant.parse("2026-08-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(SESSION));
        userRepository = mock(UserRepository.class);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        SchedulingProperties props = new SchedulingProperties(); // default teacherTimezone = Europe/Madrid
        emailService = new BookingEmailService(mailSender, "noreply@kuky.es", userRepository, props, true);
    }

    private void studentHasTimezone(String email, String timezone) {
        User student = new User();
        student.setEmail(email);
        student.setTimezone(timezone);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(student));
    }

    private MimeMessage[] captureSentMessages(int expectedCount) {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(expectedCount)).send(captor.capture());
        return captor.getAllValues().toArray(new MimeMessage[0]);
    }

    private String icsTextOf(MimeMessage message) throws Exception {
        // In production, JavaMailSenderImpl.send() calls saveChanges() before handing off to
        // Transport, which syncs each MimeBodyPart's Content-Type header from its DataHandler.
        // The mocked sender in this test never does that, so we replicate it here.
        message.saveChanges();
        Multipart mp = (Multipart) message.getContent();
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart part = mp.getBodyPart(i);
            if (part.getContentType() != null && part.getContentType().startsWith("text/calendar")) {
                return new String(part.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String textBodyOf(MimeMessage message) throws Exception {
        // As in icsTextOf: read the raw bytes rather than relying on getContent()'s String
        // coercion, since the bare test Session has no DataContentHandler registered for
        // text/plain and would otherwise hand back an InputStream, not a String. The text part
        // is whichever one isn't the calendar attachment.
        message.saveChanges();
        Object content = message.getContent();
        if (content instanceof String s) {
            return s;
        }
        Multipart mp = (Multipart) content;
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart part = mp.getBodyPart(i);
            String contentType = part.getContentType();
            if (contentType == null || !contentType.startsWith("text/calendar")) {
                return new String(part.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Test
    void sendConfirmation_attachesRequestIcsToStudentAndTeacherEmails() throws Exception {
        emailService.sendConfirmation("student@example.com", "paula@kuky.es", bookingId,
                slotStart, 50, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(2);

        for (MimeMessage message : sent) {
            String ics = icsTextOf(message);
            assertThat(ics).isNotNull();
            assertThat(ics).contains("METHOD:REQUEST");
            assertThat(ics).contains("UID:booking-" + bookingId + "@kuky.es");
        }
    }

    @Test
    void sendCancellation_studentInitiated_attachesCancelIcsToBothStudentAndTeacher() throws Exception {
        emailService.sendCancellation("paula@kuky.es", "student@example.com", bookingId,
                slotStart, 50, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(2);

        for (MimeMessage message : sent) {
            String ics = icsTextOf(message);
            assertThat(ics).isNotNull();
            assertThat(ics).contains("METHOD:CANCEL");
            assertThat(ics).contains("SEQUENCE:1");
            assertThat(ics).contains("UID:booking-" + bookingId + "@kuky.es");
        }
    }

    @Test
    void sendCancellation_studentInitiated_notifiesStudentDirectly() throws Exception {
        emailService.sendCancellation("paula@kuky.es", "student@example.com", bookingId,
                slotStart, 50, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(2);
        boolean studentNotified = false;
        for (MimeMessage message : sent) {
            for (jakarta.mail.Address to : message.getAllRecipients()) {
                if (to.toString().contains("student@example.com")) {
                    studentNotified = true;
                }
            }
        }
        assertThat(studentNotified).isTrue();
    }

    @Test
    void sendCancellationByTeacher_attachesCancelIcsToBothStudentAndTeacher() throws Exception {
        emailService.sendCancellationByTeacher("student@example.com", "paula@kuky.es", bookingId,
                slotStart, 50, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(2);

        for (MimeMessage message : sent) {
            String ics = icsTextOf(message);
            assertThat(ics).isNotNull();
            assertThat(ics).contains("METHOD:CANCEL");
            assertThat(ics).contains("UID:booking-" + bookingId + "@kuky.es");
        }
    }

    @Test
    void sendReminderToStudent_hasNoIcsAttachment() throws Exception {
        emailService.sendReminderToStudent("student@example.com", slotStart, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(1);
        Object content = sent[0].getContent();
        assertThat(content).isInstanceOf(String.class);
    }

    @Test
    void sendReminderToTeacher_hasNoIcsAttachment() throws Exception {
        emailService.sendReminderToTeacher("paula@kuky.es", "student@example.com", slotStart, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(1);
        Object content = sent[0].getContent();
        assertThat(content).isInstanceOf(String.class);
    }

    @Test
    void sendReminderToStudent_usesStudentZone_whenStudentHasSyncedPreference() throws Exception {
        studentHasTimezone("student@example.com", "America/New_York");

        emailService.sendReminderToStudent("student@example.com", slotStart, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(1);
        String body = textBodyOf(sent[0]);
        // 2026-08-15T10:00:00Z is 06:00 in America/New_York (EDT, UTC-4).
        assertThat(body).contains("06:00 (America/New_York)");
    }

    @Test
    void sendReminderToStudent_fallsBackToTeacherZone_whenStudentHasNoSyncedPreference() throws Exception {
        emailService.sendReminderToStudent("student@example.com", slotStart, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(1);
        String body = textBodyOf(sent[0]);
        // 2026-08-15T10:00:00Z is 12:00 in Europe/Madrid (CEST, UTC+2) — the teacher-zone fallback.
        assertThat(body).contains("12:00 (Europe/Madrid)");
    }

    @Test
    void sendReminderToTeacher_alwaysUsesTeacherZone_regardlessOfStudentPreference() throws Exception {
        studentHasTimezone("student@example.com", "America/New_York");

        emailService.sendReminderToTeacher("paula@kuky.es", "student@example.com", slotStart, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(1);
        String body = textBodyOf(sent[0]);
        assertThat(body).contains("12:00 (Europe/Madrid)");
        assertThat(body).doesNotContain("America/New_York");
    }

    @Test
    void sendCompanionStudentAttached_sendsEmailWithIcsToTheGivenRecipient() throws Exception {
        emailService.sendCompanionStudentAttached("second@example.com", bookingId,
                slotStart, 60, "https://zoom.us/j/123");

        MimeMessage[] sent = captureSentMessages(1);
        MimeMessage message = sent[0];

        boolean secondStudentNotified = false;
        for (jakarta.mail.Address to : message.getAllRecipients()) {
            if (to.toString().contains("second@example.com")) {
                secondStudentNotified = true;
            }
        }
        assertThat(secondStudentNotified).isTrue();

        String ics = icsTextOf(message);
        assertThat(ics).isNotNull();
        assertThat(ics).contains("METHOD:REQUEST");
        assertThat(ics).contains("UID:booking-" + bookingId + "@kuky.es");

        String body = textBodyOf(message);
        assertThat(body).contains("https://zoom.us/j/123");
    }

    @Test
    void sendConfirmation_stillSendsEmail_whenMailSenderThrowsOnFirstAttempt() {
        when(mailSender.createMimeMessage())
                .thenThrow(new org.springframework.mail.MailPreparationException("boom"))
                .thenAnswer(inv -> new MimeMessage(SESSION));

        emailService.sendConfirmation("student@example.com", "paula@kuky.es", bookingId,
                slotStart, 50, "https://zoom.us/j/123");

        // First send failed (exception swallowed), second (teacher) still went through.
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void mailDisabled_skipsSendingEntirely() {
        BookingEmailService disabledEmailService = new BookingEmailService(
                mailSender, "noreply@kuky.es", userRepository, new SchedulingProperties(), false);

        disabledEmailService.sendConfirmation("student@example.com", "paula@kuky.es", bookingId,
                slotStart, 50, "https://zoom.us/j/123");

        verifyNoInteractions(mailSender);
    }
}
