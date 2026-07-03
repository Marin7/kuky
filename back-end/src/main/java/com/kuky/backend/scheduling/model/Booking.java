package com.kuky.backend.scheduling.model;

import java.time.Instant;
import java.util.UUID;

public class Booking {

    private UUID id;
    private UUID userId;
    private Instant slotStart;
    private int durationMinutes;
    private String status;
    private String zoomMeetingId;
    private String zoomJoinUrl;
    private Instant createdAt;
    private Instant cancelledAt;
    private Instant reminderSentAt;
    private boolean noShow;
    private UUID companionStudentId;
    private Boolean companionStudentNoShow;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Instant getSlotStart() { return slotStart; }
    public void setSlotStart(Instant slotStart) { this.slotStart = slotStart; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getZoomMeetingId() { return zoomMeetingId; }
    public void setZoomMeetingId(String zoomMeetingId) { this.zoomMeetingId = zoomMeetingId; }
    public String getZoomJoinUrl() { return zoomJoinUrl; }
    public void setZoomJoinUrl(String zoomJoinUrl) { this.zoomJoinUrl = zoomJoinUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public Instant getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(Instant reminderSentAt) { this.reminderSentAt = reminderSentAt; }
    public boolean isNoShow() { return noShow; }
    public void setNoShow(boolean noShow) { this.noShow = noShow; }
    public UUID getCompanionStudentId() { return companionStudentId; }
    public void setCompanionStudentId(UUID companionStudentId) { this.companionStudentId = companionStudentId; }
    public Boolean getCompanionStudentNoShow() { return companionStudentNoShow; }
    public void setCompanionStudentNoShow(Boolean companionStudentNoShow) { this.companionStudentNoShow = companionStudentNoShow; }

    public Instant getSlotEnd() {
        return slotStart.plusSeconds((long) durationMinutes * 60);
    }
}
