package com.kuky.backend.resources.controller;

import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.resources.dto.CatalogResponse;
import com.kuky.backend.resources.dto.ContentResponse;
import com.kuky.backend.resources.dto.ResourceDetailDto;
import com.kuky.backend.resources.service.CatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resources")
public class CatalogController {

    private final CatalogService catalogService;
    private final UserRepository userRepository;

    public CatalogController(CatalogService catalogService, UserRepository userRepository) {
        this.catalogService = catalogService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<CatalogResponse> getCatalog(@AuthenticationPrincipal String email) {
        Optional<UUID> userId = resolveUserId(email);
        return ResponseEntity.ok(catalogService.getCatalog(userId));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ResourceDetailDto> getResource(
            @PathVariable String slug,
            @AuthenticationPrincipal String email) {
        Optional<UUID> userId = resolveUserId(email);
        return ResponseEntity.ok(catalogService.getResource(slug, userId));
    }

    @GetMapping("/{slug}/content")
    public ResponseEntity<ContentResponse> getContent(
            @PathVariable String slug,
            @AuthenticationPrincipal String email) {
        Optional<UUID> userId = resolveUserId(email);
        return ResponseEntity.ok(catalogService.getContent(slug, userId));
    }

    private Optional<UUID> resolveUserId(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .map(u -> u.getId());
    }
}
