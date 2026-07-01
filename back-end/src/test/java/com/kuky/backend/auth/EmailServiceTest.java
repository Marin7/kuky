package com.kuky.backend.auth;

import com.kuky.backend.auth.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@kuky.es");
        ReflectionTestUtils.setField(emailService, "baseUrl", "https://kuky.es");
    }

    @Test
    void sendStudentGrantedEmail_sendsMessageToRecipientWithNonEmptyContent() {
        emailService.sendStudentGrantedEmail("alumno@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("alumno@example.com");
        assertThat(message.getFrom()).isEqualTo("noreply@kuky.es");
        assertThat(message.getSubject()).isNotBlank();
        assertThat(message.getText()).isNotBlank();
    }

    @Test
    void sendStudentRevokedEmail_sendsMessageToRecipientWithNonEmptyContent() {
        emailService.sendStudentRevokedEmail("exalumno@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("exalumno@example.com");
        assertThat(message.getFrom()).isEqualTo("noreply@kuky.es");
        assertThat(message.getSubject()).isNotBlank();
        assertThat(message.getText()).isNotBlank();
    }
}
