package com.kuky.backend.auth.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateInterestsRequest(
        List<String> interests,
        @Size(max = 280) String interestsNote
) {}
