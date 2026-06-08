package com.kuky.backend.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(UUID id, String email, Instant createdAt) {}
