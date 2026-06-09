package com.kuky.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class SchedulingProperties {

    private Scheduling scheduling = new Scheduling();
    private Zoom zoom = new Zoom();

    public Scheduling getScheduling() { return scheduling; }
    public void setScheduling(Scheduling scheduling) { this.scheduling = scheduling; }
    public Zoom getZoom() { return zoom; }
    public void setZoom(Zoom zoom) { this.zoom = zoom; }

    public static class Scheduling {
        private String teacherTimezone = "Europe/Madrid";
        private String teacherEmail = "noreply@kuky.es";
        private String dayStart = "09:00";
        private String dayEnd = "18:00";
        private String lunchBreakStart = "12:00";
        private String lunchBreakEnd = "14:00";
        private int classDurationMinutes = 60;
        private int minLeadHours = 24;
        private int cancelCutoffHours = 24;

        public String getTeacherTimezone() { return teacherTimezone; }
        public void setTeacherTimezone(String teacherTimezone) { this.teacherTimezone = teacherTimezone; }
        public String getTeacherEmail() { return teacherEmail; }
        public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }
        public String getDayStart() { return dayStart; }
        public void setDayStart(String dayStart) { this.dayStart = dayStart; }
        public String getDayEnd() { return dayEnd; }
        public void setDayEnd(String dayEnd) { this.dayEnd = dayEnd; }
        public String getLunchBreakStart() { return lunchBreakStart; }
        public void setLunchBreakStart(String lunchBreakStart) { this.lunchBreakStart = lunchBreakStart; }
        public String getLunchBreakEnd() { return lunchBreakEnd; }
        public void setLunchBreakEnd(String lunchBreakEnd) { this.lunchBreakEnd = lunchBreakEnd; }
        public int getClassDurationMinutes() { return classDurationMinutes; }
        public void setClassDurationMinutes(int classDurationMinutes) { this.classDurationMinutes = classDurationMinutes; }
        public int getMinLeadHours() { return minLeadHours; }
        public void setMinLeadHours(int minLeadHours) { this.minLeadHours = minLeadHours; }
        public int getCancelCutoffHours() { return cancelCutoffHours; }
        public void setCancelCutoffHours(int cancelCutoffHours) { this.cancelCutoffHours = cancelCutoffHours; }
    }

    public static class Zoom {
        private String accountId = "";
        private String clientId = "";
        private String clientSecret = "";
        private String userId = "me";

        public String getAccountId() { return accountId; }
        public void setAccountId(String accountId) { this.accountId = accountId; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}
