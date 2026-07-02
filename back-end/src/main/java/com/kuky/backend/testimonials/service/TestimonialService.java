package com.kuky.backend.testimonials.service;

import com.kuky.backend.admin.dto.TestimonialAdminResponse;
import com.kuky.backend.auth.model.User;
import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.config.SchedulingProperties;
import com.kuky.backend.testimonials.dto.MyTestimonialResponse;
import com.kuky.backend.testimonials.dto.TestimonialResponse;
import com.kuky.backend.testimonials.exception.TestimonialNotFoundException;
import com.kuky.backend.testimonials.model.Testimonial;
import com.kuky.backend.testimonials.model.TestimonialStatus;
import com.kuky.backend.testimonials.repository.TestimonialRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class TestimonialService {

    private final TestimonialRepository repository;
    private final UserRepository userRepository;
    private final TestimonialEmailService emailService;
    private final SchedulingProperties props;

    public TestimonialService(TestimonialRepository repository,
                              UserRepository userRepository,
                              TestimonialEmailService emailService,
                              SchedulingProperties props) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.props = props;
    }

    public List<TestimonialResponse> listApproved() {
        return repository.findApproved().stream()
                .map(t -> new TestimonialResponse(t.getText(), t.getStudentName(), t.getDisplayOrder()))
                .toList();
    }

    public MyTestimonialResponse submit(String userEmail, String text) {
        User user = userRepository.findByEmailIgnoreCase(userEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        String studentName = displayName(user);
        Testimonial saved = repository.upsertByUser(user.getId(), studentName, text);
        emailService.sendSubmittedNotificationToTeacher(props.getScheduling().getTeacherEmail(), studentName);

        return toMyTestimonialResponse(saved);
    }

    public Optional<MyTestimonialResponse> getMyTestimonial(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
        return repository.findByUserId(user.getId()).map(this::toMyTestimonialResponse);
    }

    private MyTestimonialResponse toMyTestimonialResponse(Testimonial t) {
        return new MyTestimonialResponse(t.getText(), t.getStatus(), t.getSubmittedAt());
    }

    // --- admin review/curation -------------------------------------------------

    public List<TestimonialAdminResponse> listAll() {
        return repository.findAll().stream().map(this::toAdminResponse).toList();
    }

    public TestimonialAdminResponse approve(UUID id) {
        requireExists(id);
        return toAdminResponse(repository.setStatus(id, TestimonialStatus.APPROVED));
    }

    public TestimonialAdminResponse reject(UUID id) {
        requireExists(id);
        return toAdminResponse(repository.setStatus(id, TestimonialStatus.REJECTED));
    }

    public TestimonialAdminResponse unpublish(UUID id) {
        requireExists(id);
        return toAdminResponse(repository.setStatus(id, TestimonialStatus.UNPUBLISHED));
    }

    public TestimonialAdminResponse updateText(UUID id, String text) {
        requireExists(id);
        return toAdminResponse(repository.setText(id, text));
    }

    public List<TestimonialAdminResponse> reorder(List<UUID> orderedIds) {
        repository.reorder(orderedIds);
        return listAll();
    }

    public void delete(UUID id) {
        requireExists(id);
        repository.delete(id);
    }

    private void requireExists(UUID id) {
        repository.findById(id).orElseThrow(() -> new TestimonialNotFoundException("Testimonio no encontrado."));
    }

    private TestimonialAdminResponse toAdminResponse(Testimonial t) {
        return new TestimonialAdminResponse(t.getId(), t.getText(), t.getStudentName(), t.getStatus(),
                t.getDisplayOrder(), t.getSubmittedAt(), t.getReviewedAt());
    }

    /** Same precedence as the front-end's studentDisplayName helper. */
    private String displayName(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            return user.getFirstName() + " " + user.getLastName();
        }
        if (user.getFirstName() != null) {
            return user.getFirstName();
        }
        if (user.getUsername() != null) {
            return "@" + user.getUsername();
        }
        return user.getEmail().split("@")[0];
    }
}
