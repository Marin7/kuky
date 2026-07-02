package com.kuky.backend.scheduling.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class BookingEmailService {

    private static final Logger log = LoggerFactory.getLogger(BookingEmailService.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    private static final String ICS_FILENAME = "clase.ics";
    private static final String EVENT_SUMMARY = "Clase de español con Paula";

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final IcsEventFactory icsFactory = new IcsEventFactory();

    public BookingEmailService(JavaMailSender mailSender,
                               @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendConfirmation(String studentEmail, String teacherEmail, UUID bookingId,
                                 Instant slotStart, int durationMinutes, String joinUrl) {
        String when = FMT.format(slotStart);
        String subject = "Reserva confirmada — Español con Paula";
        String body = """
                Tu clase ha sido reservada.

                Fecha y hora: %s
                Duración: %d minutos
                Enlace de Zoom: %s

                ¡Hasta pronto!
                """.formatted(when, durationMinutes, joinUrl);

        byte[] studentIcs = buildIcs(IcsEventFactory.Method.REQUEST, bookingId, slotStart, durationMinutes,
                "Enlace de Zoom: " + joinUrl, joinUrl, teacherEmail, studentEmail);
        sendQuietly(studentEmail, subject, body, studentIcs, IcsEventFactory.Method.REQUEST);

        byte[] teacherIcs = buildIcs(IcsEventFactory.Method.REQUEST, bookingId, slotStart, durationMinutes,
                "Estudiante: " + studentEmail + "\nEnlace de Zoom: " + joinUrl, joinUrl, teacherEmail, studentEmail);
        sendQuietly(teacherEmail, "Nueva reserva: " + studentEmail + " — " + when,
                "Un estudiante ha reservado una clase.\n\nEstudiante: " + studentEmail
                + "\nFecha y hora: " + when + "\nEnlace de Zoom: " + joinUrl,
                teacherIcs, IcsEventFactory.Method.REQUEST);
    }

    /** Student-initiated cancellation: notify the teacher, and confirm the cancellation to the student. */
    public void sendCancellation(String teacherEmail, String studentEmail, UUID bookingId,
                                 Instant slotStart, int durationMinutes, String joinUrl) {
        String when = FMT.format(slotStart);

        byte[] teacherIcs = buildIcs(IcsEventFactory.Method.CANCEL, bookingId, slotStart, durationMinutes,
                "Estudiante: " + studentEmail, joinUrl, teacherEmail, studentEmail);
        sendQuietly(teacherEmail, "Clase cancelada: " + studentEmail + " — " + when,
                "El estudiante " + studentEmail + " ha cancelado la clase del " + when + ".",
                teacherIcs, IcsEventFactory.Method.CANCEL);

        byte[] studentIcs = buildIcs(IcsEventFactory.Method.CANCEL, bookingId, slotStart, durationMinutes,
                "Cancelada", joinUrl, teacherEmail, studentEmail);
        sendQuietly(studentEmail, "Clase cancelada — Español con Paula",
                "Has cancelado tu clase del " + when + ".",
                studentIcs, IcsEventFactory.Method.CANCEL);
    }

    /** Teacher-initiated cancellation: notify the student their class was cancelled, and confirm to the teacher. */
    public void sendCancellationByTeacher(String studentEmail, String teacherEmail, UUID bookingId,
                                          Instant slotStart, int durationMinutes, String joinUrl) {
        String when = FMT.format(slotStart);
        String subject = "Clase cancelada — Español con Paula";
        String body = """
                Tu clase del %s ha sido cancelada por la profesora.

                Si lo deseas, puedes reservar otra hora desde la web.

                Disculpa las molestias.
                """.formatted(when);

        byte[] studentIcs = buildIcs(IcsEventFactory.Method.CANCEL, bookingId, slotStart, durationMinutes,
                "Cancelada por la profesora", joinUrl, teacherEmail, studentEmail);
        sendQuietly(studentEmail, subject, body, studentIcs, IcsEventFactory.Method.CANCEL);

        byte[] teacherIcs = buildIcs(IcsEventFactory.Method.CANCEL, bookingId, slotStart, durationMinutes,
                "Estudiante: " + studentEmail, joinUrl, teacherEmail, studentEmail);
        sendQuietly(teacherEmail, "Clase cancelada: " + studentEmail + " — " + when,
                "Has cancelado la clase de " + studentEmail + " del " + when + ".",
                teacherIcs, IcsEventFactory.Method.CANCEL);
    }

    /** 24h-before reminder to the student. No calendar attachment — the student already has one from the confirmation. */
    public void sendReminderToStudent(String studentEmail, Instant slotStart, String joinUrl) {
        String when = FMT.format(slotStart);
        String subject = "Recordatorio: tu clase es mañana — Español con Paula";
        String body = """
                Este es un recordatorio de que tu clase es en aproximadamente 24 horas.

                Fecha y hora: %s
                Enlace de Zoom: %s

                ¡Hasta pronto!
                """.formatted(when, joinUrl);
        sendQuietly(studentEmail, subject, body, null, null);
    }

    /** 24h-before reminder to the teacher, identifying the student and class. No calendar attachment. */
    public void sendReminderToTeacher(String teacherEmail, String studentEmail, Instant slotStart, String joinUrl) {
        String when = FMT.format(slotStart);
        String subject = "Recordatorio: clase con " + studentEmail + " — " + when;
        String body = "Recordatorio de que tienes una clase en aproximadamente 24 horas.\n\n"
                + "Estudiante: " + studentEmail
                + "\nFecha y hora: " + when
                + "\nEnlace de Zoom: " + joinUrl;
        sendQuietly(teacherEmail, subject, body, null, null);
    }

    private byte[] buildIcs(IcsEventFactory.Method method, UUID bookingId, Instant slotStart, int durationMinutes,
                            String description, String joinUrl, String organizerEmail, String attendeeEmail) {
        try {
            return icsFactory.build(method, bookingId, slotStart, durationMinutes,
                    EVENT_SUMMARY, description, joinUrl, organizerEmail, attendeeEmail);
        } catch (RuntimeException e) {
            log.warn("BookingEmailService — failed to build ics attachment for booking {}: {}", bookingId, e.getMessage());
            return null;
        }
    }

    private void sendQuietly(String to, String subject, String text, byte[] icsBytes, IcsEventFactory.Method method) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, icsBytes != null);
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            if (icsBytes != null) {
                try {
                    String contentType = "text/calendar; charset=UTF-8; method=" + method.name();
                    helper.addAttachment(ICS_FILENAME, () -> new ByteArrayInputStream(icsBytes), contentType);
                } catch (Exception e) {
                    log.warn("BookingEmailService — failed to attach ics to email to {}: {}", to, e.getMessage());
                }
            }

            mailSender.send(mimeMessage);
        } catch (MailException | jakarta.mail.MessagingException e) {
            log.warn("BookingEmailService — failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
