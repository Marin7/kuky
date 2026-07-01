package com.kuky.backend.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("local")
class ResourcesControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getCatalog_publicNoAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.freeResources").isArray())
                .andExpect(jsonPath("$.paidResources").isArray())
                .andExpect(jsonPath("$.bundles").isArray());
    }

    @Test
    void getContent_paidResourceNoAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/resources/pack-vocabulario-a1/content"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("RESOURCE_LOCKED"));
    }

    @Test
    void getContent_freeResourceNoAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/resources/saludos-y-presentaciones/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assets").isArray());
    }

    @Test
    void purchase_notAuthenticated_returns401() throws Exception {
        String body = "{\"itemType\":\"RESOURCE\",\"slug\":\"pack-vocabulario-a1\"}";
        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void purchase_authenticated_thenContentAccessible() throws Exception {
        // Register a test user
        String testEmail = "test-resources-" + UUID.randomUUID() + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, status, role, gdpr_consent) VALUES (gen_random_uuid(), ?, 'hash', 'ACTIVE', 'STUDENT', true)",
                testEmail
        );

        // Purchase
        String purchaseBody = "{\"itemType\":\"RESOURCE\",\"slug\":\"pack-vocabulario-a1\"}";
        mockMvc.perform(post("/api/v1/purchases")
                        .with(authentication(new UsernamePasswordAuthenticationToken(testEmail, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")))))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptReference").isNotEmpty())
                .andExpect(jsonPath("$.grantedResourceSlugs[0]").value("pack-vocabulario-a1"));

        // Content now accessible
        mockMvc.perform(get("/api/v1/resources/pack-vocabulario-a1/content")
                        .with(authentication(new UsernamePasswordAuthenticationToken(testEmail, null, List.of()))))
                .andExpect(status().isOk());

        // Receipt only accessible to owner
        mockMvc.perform(get("/api/v1/purchases")
                        .with(authentication(new UsernamePasswordAuthenticationToken(testEmail, null, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchases").isArray());

        // Clean up
        jdbcTemplate.update("DELETE FROM users WHERE email = ?", testEmail);
    }
}
