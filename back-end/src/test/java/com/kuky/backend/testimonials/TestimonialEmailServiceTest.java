package com.kuky.backend.testimonials;

import com.kuky.backend.testimonials.service.TestimonialEmailService;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TestimonialEmailServiceTest {

    @Mock private JavaMailSender mailSender;

    private TestimonialEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new TestimonialEmailService(mailSender, "noreply@kuky.es", true);
    }

    @Test
    void sendSubmittedNotificationToTeacher_sendsMessageWithNonEmptyContent() {
        emailService.sendSubmittedNotificationToTeacher("paula@kuky.es", "Ana Popescu");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("paula@kuky.es");
        assertThat(message.getFrom()).isEqualTo("noreply@kuky.es");
        assertThat(message.getSubject()).isNotBlank();
        assertThat(message.getText()).contains("Ana Popescu");
    }

    @Test
    void mailDisabled_skipsSendingEntirely() {
        TestimonialEmailService disabledEmailService = new TestimonialEmailService(mailSender, "noreply@kuky.es", false);

        disabledEmailService.sendSubmittedNotificationToTeacher("paula@kuky.es", "Ana Popescu");

        verifyNoInteractions(mailSender);
    }
}
