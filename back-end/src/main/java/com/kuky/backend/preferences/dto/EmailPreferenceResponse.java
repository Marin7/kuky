package com.kuky.backend.preferences.dto;

import com.kuky.backend.preferences.model.EmailPreferenceType;

/** One email option and whether this account has opted in to it. */
public record EmailPreferenceResponse(EmailPreferenceType type, boolean enabled) {}
