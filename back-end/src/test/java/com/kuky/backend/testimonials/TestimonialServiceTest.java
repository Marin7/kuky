package com.kuky.backend.testimonials;

import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.testimonials.dto.MyTestimonialResponse;
import com.kuky.backend.testimonials.exception.TestimonialNotFoundException;
import com.kuky.backend.testimonials.model.Testimonial;
import com.kuky.backend.testimonials.model.TestimonialStatus;
import com.kuky.backend.testimonials.repository.TestimonialRepository;
import com.kuky.backend.testimonials.service.TestimonialEmailService;
import com.kuky.backend.testimonials.service.TestimonialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestimonialServiceTest {

    @Mock private TestimonialRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private TestimonialEmailService emailService;

    private TestimonialService service;

    private final UUID userId = UUID.randomUUID();
    private static final String EMAIL = "ana@example.com";

    @BeforeEach
    void setUp() {
        SchedulingProperties props = new SchedulingProperties();
        props.getScheduling().setTeacherEmail("paula@kuky.es");
        service = new TestimonialService(repository, userRepository, emailService, props);

        User user = new User();
        user.setId(userId);
        user.setEmail(EMAIL);
        user.setFirstName("Ana");
        user.setLastName("Popescu");
        lenient().when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
    }

    @Test
    void submit_createsPendingTestimonialWithSnapshottedName_andNotifiesTeacher() {
        when(repository.upsertByUser(eq(userId), eq("Ana Popescu"), eq("Great classes!")))
                .thenReturn(testimonial(TestimonialStatus.PENDING, "Great classes!"));

        MyTestimonialResponse result = service.submit(EMAIL, "Great classes!");

        assertThat(result.status()).isEqualTo(TestimonialStatus.PENDING);
        assertThat(result.text()).isEqualTo("Great classes!");
        verify(emailService).sendSubmittedNotificationToTeacher("paula@kuky.es", "Ana Popescu");
    }

    @Test
    void submit_resubmission_callsUpsertNotInsert() {
        when(repository.upsertByUser(eq(userId), eq("Ana Popescu"), eq("Updated text")))
                .thenReturn(testimonial(TestimonialStatus.PENDING, "Updated text"));

        service.submit(EMAIL, "Updated text");

        verify(repository).upsertByUser(userId, "Ana Popescu", "Updated text");
    }

    @Test
    void getMyTestimonial_returnsEmpty_whenNeverSubmitted() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(userWithId()));
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        Optional<MyTestimonialResponse> result = service.getMyTestimonial(EMAIL);

        assertThat(result).isEmpty();
    }

    @Test
    void getMyTestimonial_returnsCurrentStatus_whenSubmitted() {
        when(repository.findByUserId(userId))
                .thenReturn(Optional.of(testimonial(TestimonialStatus.APPROVED, "Great classes!")));

        Optional<MyTestimonialResponse> result = service.getMyTestimonial(EMAIL);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(TestimonialStatus.APPROVED);
    }

    @Test
    void approve_setsStatusApproved() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(testimonial(TestimonialStatus.PENDING, "text")));
        when(repository.setStatus(id, TestimonialStatus.APPROVED))
                .thenReturn(testimonial(TestimonialStatus.APPROVED, "text"));

        var result = service.approve(id);

        assertThat(result.status()).isEqualTo(TestimonialStatus.APPROVED);
    }

    @Test
    void approve_unknownId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(id)).isInstanceOf(TestimonialNotFoundException.class);
    }

    @Test
    void reject_setsStatusRejected() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(testimonial(TestimonialStatus.PENDING, "text")));
        when(repository.setStatus(id, TestimonialStatus.REJECTED))
                .thenReturn(testimonial(TestimonialStatus.REJECTED, "text"));

        var result = service.reject(id);

        assertThat(result.status()).isEqualTo(TestimonialStatus.REJECTED);
    }

    @Test
    void unpublish_setsStatusUnpublished() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(testimonial(TestimonialStatus.APPROVED, "text")));
        when(repository.setStatus(id, TestimonialStatus.UNPUBLISHED))
                .thenReturn(testimonial(TestimonialStatus.UNPUBLISHED, "text"));

        var result = service.unpublish(id);

        assertThat(result.status()).isEqualTo(TestimonialStatus.UNPUBLISHED);
    }

    @Test
    void updateText_unknownId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateText(id, "new text"))
                .isInstanceOf(TestimonialNotFoundException.class);
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(TestimonialNotFoundException.class);
    }

    @Test
    void reorder_delegatesToRepository() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(repository.findAll()).thenReturn(List.of());

        service.reorder(ids);

        verify(repository).reorder(ids);
    }

    private User userWithId() {
        User user = new User();
        user.setId(userId);
        user.setEmail(EMAIL);
        return user;
    }

    private Testimonial testimonial(TestimonialStatus status, String text) {
        Testimonial t = new Testimonial();
        t.setId(UUID.randomUUID());
        t.setUserId(userId);
        t.setStudentName("Ana Popescu");
        t.setText(text);
        t.setStatus(status);
        t.setDisplayOrder(0);
        t.setSubmittedAt(Instant.now());
        return t;
    }
}
