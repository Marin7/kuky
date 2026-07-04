package com.kuky.backend.testimonials.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class TestimonialEmailService {

    private static final Logger log = LoggerFactory.getLogger(TestimonialEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean mailEnabled;

    public TestimonialEmailService(JavaMailSender mailSender,
                                   @Value("${app.mail.from}") String fromAddress,
                                   @Value("${app.mail.enabled:false}") boolean mailEnabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mailEnabled = mailEnabled;
    }

    public void sendSubmittedNotificationToTeacher(String teacherEmail, String studentName) {
        String subject = "Nuevo testimonio pendiente de revisión — Español con Paula";
        String body = """
                %s ha enviado un testimonio para revisar.

                Entra en el panel de administración para aprobarlo o rechazarlo.
                """.formatted(studentName);
        sendQuietly(teacherEmail, subject, body);
    }

    private void sendQuietly(String to, String subject, String text) {
        if (!mailEnabled) {
            log.debug("TestimonialEmailService — mail disabled, skipping send to {}", to);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
        } catch (MailException e) {
            log.warn("TestimonialEmailService — failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
