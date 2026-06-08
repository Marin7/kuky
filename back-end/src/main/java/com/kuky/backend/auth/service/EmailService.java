package com.kuky.backend.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url}")
    private String baseUrl;

    @Value("${app.mail.from:noreply@kuky.es}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = baseUrl + "/cuenta?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Recupera tu contraseña — Kuky");
        message.setText(
                "Hola,\n\n" +
                "Haz clic en el siguiente enlace para restablecer tu contraseña:\n\n" +
                resetUrl + "\n\n" +
                "Este enlace expira en 1 hora. Si no solicitaste este correo, ignóralo.\n\n" +
                "Saludos,\nEl equipo de Kuky"
        );

        mailSender.send(message);
    }
}
