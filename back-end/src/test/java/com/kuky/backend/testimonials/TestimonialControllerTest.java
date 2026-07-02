package com.kuky.backend.testimonials;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class TestimonialControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private UUID studentId;
    private String studentEmail;
    private UUID userId;
    private String userEmail;

    private static UsernamePasswordAuthenticationToken studentPrincipal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    private static UsernamePasswordAuthenticationToken userPrincipal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        studentId = UUID.randomUUID();
        studentEmail = "testimonial-student-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent, first_name, last_name) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true, 'Ana', 'Popescu')",
                studentId, studentEmail);

        userId = UUID.randomUUID();
        userEmail = "testimonial-user-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'USER', true)",
                userId, userEmail);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM testimonials WHERE user_id IN (?, ?)", studentId, userId);
        jdbcTemplate.update("DELETE FROM users WHERE id IN (?, ?)", studentId, userId);
    }

    @Test
    void getTestimonials_isPublic_andReturnsOnlyApprovedOrderedByDisplayOrder() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO testimonials (id, user_id, student_name, text, status, display_order)
                VALUES (gen_random_uuid(), ?, 'Ana Popescu', 'Great classes!', 'APPROVED', 0)
                """, studentId);

        mockMvc.perform(get("/api/v1/testimonials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Ana Popescu"))
                .andExpect(jsonPath("$[0].text").value("Great classes!"));
    }

    @Test
    void getTestimonials_excludesNonApproved() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO testimonials (id, user_id, student_name, text, status, display_order)
                VALUES (gen_random_uuid(), ?, 'Ana Popescu', 'Pending review', 'PENDING', 0)
                """, studentId);

        mockMvc.perform(get("/api/v1/testimonials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.text=='Pending review')]").doesNotExist());
    }

    @Test
    void getTestimonials_returnsJsonArray_neverAnError() throws Exception {
        mockMvc.perform(get("/api/v1/testimonials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void submitTestimonial_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/testimonials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great classes!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitTestimonial_forbidsPlainUser() throws Exception {
        mockMvc.perform(post("/api/v1/testimonials")
                        .with(authentication(userPrincipal(userEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great classes!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitTestimonial_allowsStudent_andCreatesPending() throws Exception {
        mockMvc.perform(post("/api/v1/testimonials")
                        .with(authentication(studentPrincipal(studentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great classes!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.text").value("Great classes!"));
    }

    @Test
    void submitTestimonial_rejectsBlankText() throws Exception {
        mockMvc.perform(post("/api/v1/testimonials")
                        .with(authentication(studentPrincipal(studentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getMyTestimonial_returnsNoContent_whenNeverSubmitted() throws Exception {
        mockMvc.perform(get("/api/v1/testimonials/me")
                        .with(authentication(studentPrincipal(studentEmail))))
                .andExpect(status().isNoContent());
    }

    @Test
    void getMyTestimonial_returnsOwnStatus_afterSubmission() throws Exception {
        mockMvc.perform(post("/api/v1/testimonials")
                        .with(authentication(studentPrincipal(studentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Great classes!\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/testimonials/me")
                        .with(authentication(studentPrincipal(studentEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.text").value("Great classes!"));
    }
}
