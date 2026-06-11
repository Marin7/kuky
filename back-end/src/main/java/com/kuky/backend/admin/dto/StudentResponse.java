package com.kuky.backend.admin.dto;

import java.util.UUID;

public record StudentResponse(UUID id, String email, String firstName, String lastName, String username) {}
