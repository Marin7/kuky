package com.kuky.backend.scheduling.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class BookingEmailService {

    private static final Logger log = LoggerFactory.getLogger(BookingEmailService.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public BookingEmailService(JavaMailSender mailSender,
                               @org.springframework.beans.factory.annotation.Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendConfirmation(String studentEmail, String teacherEmail,
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

        sendQuietly(studentEmail, subject, body);
        sendQuietly(teacherEmail, "Nueva reserva: " + studentEmail + " — " + when,
                "Un estudiante ha reservado una clase.\n\nEstudiante: " + studentEmail
                + "\nFecha y hora: " + when + "\nEnlace de Zoom: " + joinUrl);
    }

    public void sendCancellation(String teacherEmail, String studentEmail, Instant slotStart) {
        String when = FMT.format(slotStart);
        String subject = "Clase cancelada: " + studentEmail + " — " + when;
        String body = "El estudiante " + studentEmail + " ha cancelado la clase del " + when + ".";
        sendQuietly(teacherEmail, subject, body);
    }

    /** Teacher-initiated cancellation: notify the student their class was cancelled. */
    public void sendCancellationByTeacher(String studentEmail, String teacherEmail, Instant slotStart) {
        String when = FMT.format(slotStart);
        String subject = "Clase cancelada — Español con Paula";
        String body = """
                Tu clase del %s ha sido cancelada por la profesora.

                Si lo deseas, puedes reservar otra hora desde la web.

                Disculpa las molestias.
                """.formatted(when);
        sendQuietly(studentEmail, subject, body);
        sendQuietly(teacherEmail, "Clase cancelada: " + studentEmail + " — " + when,
                "Has cancelado la clase de " + studentEmail + " del " + when + ".");
    }

    private void sendQuietly(String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
        } catch (MailException e) {
            log.warn("BookingEmailService — failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
