package com.kuky.backend.admin.dto;

/** {@code studentRole} is optional — {@code "BOOKING_STUDENT"} (default when omitted) or {@code "COMPANION"}. */
public record SetNoShowRequest(boolean noShow, String studentRole) {
    public String studentRoleOrDefault() {
        return studentRole == null ? "BOOKING_STUDENT" : studentRole;
    }
}
