package com.kuky.backend.admin;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class TestimonialAdminControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private String adminEmail;
    private UUID studentId;
    private UUID testimonialId;

    private static UsernamePasswordAuthenticationToken adminPrincipal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static UsernamePasswordAuthenticationToken studentPrincipal(String email) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        adminEmail = "testimonial-admin-" + UUID.randomUUID() + "@kuky.es";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'ADMIN', true)",
                adminEmail);

        studentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                studentId, "testimonial-admin-student-" + UUID.randomUUID() + "@example.com");

        testimonialId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO testimonials (id, user_id, student_name, text, status, display_order)
                VALUES (?, ?, 'Ana Popescu', 'Great classes!', 'PENDING', 0)
                """, testimonialId, studentId);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM testimonials WHERE user_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", studentId);
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", adminEmail);
    }

    @Test
    void listAll_returnsEveryStatus_forAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/testimonials")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + testimonialId + "')]").exists());
    }

    @Test
    void listAll_forbidden_forNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/testimonials")
                        .with(authentication(studentPrincipal("someone@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_transitionsPendingToApproved() throws Exception {
        mockMvc.perform(post("/api/v1/admin/testimonials/" + testimonialId + "/approve")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM testimonials WHERE id = ?", String.class, testimonialId);
        org.junit.jupiter.api.Assertions.assertEquals("APPROVED", status);
    }

    @Test
    void reject_transitionsPendingToRejected() throws Exception {
        mockMvc.perform(post("/api/v1/admin/testimonials/" + testimonialId + "/reject")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void unpublish_transitionsApprovedToUnpublished() throws Exception {
        jdbcTemplate.update("UPDATE testimonials SET status = 'APPROVED' WHERE id = ?", testimonialId);

        mockMvc.perform(post("/api/v1/admin/testimonials/" + testimonialId + "/unpublish")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNPUBLISHED"));
    }

    @Test
    void updateText_editsWithoutChangingStatus() throws Exception {
        mockMvc.perform(put("/api/v1/admin/testimonials/" + testimonialId)
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Edited text\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Edited text"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void reorder_reassignsDisplayOrder() throws Exception {
        UUID secondId = UUID.randomUUID();
        UUID secondStudentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (?, ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                secondStudentId, "testimonial-admin-student2-" + UUID.randomUUID() + "@example.com");
        jdbcTemplate.update("""
                INSERT INTO testimonials (id, user_id, student_name, text, status, display_order)
                VALUES (?, ?, 'Maria Ionescu', 'Also great!', 'APPROVED', 1)
                """, secondId, secondStudentId);

        mockMvc.perform(put("/api/v1/admin/testimonials/reorder")
                        .with(authentication(adminPrincipal(adminEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderedIds\":[\"" + secondId + "\",\"" + testimonialId + "\"]}"))
                .andExpect(status().isOk());

        Integer firstOrder = jdbcTemplate.queryForObject(
                "SELECT display_order FROM testimonials WHERE id = ?", Integer.class, secondId);
        org.junit.jupiter.api.Assertions.assertEquals(0, firstOrder);

        jdbcTemplate.update("DELETE FROM testimonials WHERE id = ?", secondId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", secondStudentId);
    }

    @Test
    void delete_removesTestimonial() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/testimonials/" + testimonialId)
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNoContent());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM testimonials WHERE id = ?", Integer.class, testimonialId);
        org.junit.jupiter.api.Assertions.assertEquals(0, count);
    }

    @Test
    void approve_returns404_forUnknownId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/testimonials/" + UUID.randomUUID() + "/approve")
                        .with(authentication(adminPrincipal(adminEmail))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TESTIMONIAL_NOT_FOUND"));
    }
}
