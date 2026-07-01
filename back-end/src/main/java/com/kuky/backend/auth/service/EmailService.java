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

    public void sendActivationEmail(String toEmail, String activationToken) {
        String activationUrl = baseUrl + "/cuenta?activateToken=" + activationToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Activa tu cuenta — Kuky");
        message.setText(
                "¡Hola!\n\n" +
                "Haz clic en el siguiente enlace para activar tu cuenta:\n\n" +
                activationUrl + "\n\n" +
                "Este enlace expira en 24 horas. Si no creaste esta cuenta, ignora este correo.\n\n" +
                "Saludos,\nEl equipo de Kuky"
        );

        mailSender.send(message);
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

    public void sendStudentGrantedEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Ya eres alumno/a — Kuky");
        message.setText(
                "¡Hola!\n\n" +
                "Paula te ha dado acceso de alumno/a. Ya puedes reservar clases, " +
                "comprar y desbloquear recursos, y acceder a tus tareas y presentaciones.\n\n" +
                "Saludos,\nEl equipo de Kuky"
        );

        mailSender.send(message);
    }

    public void sendStudentRevokedEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Tu acceso de alumno/a ha cambiado — Kuky");
        message.setText(
                "Hola,\n\n" +
                "Tu acceso de alumno/a ha sido retirado por Paula. Ya no podrás reservar nuevas clases, " +
                "comprar nuevos recursos ni acceder a nuevas tareas, pero tu historial sigue disponible.\n\n" +
                "Saludos,\nEl equipo de Kuky"
        );

        mailSender.send(message);
    }
}
