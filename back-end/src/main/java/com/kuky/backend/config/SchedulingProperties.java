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
        private String teacherTimezone = "Europe/Bucharest";
        private String teacherEmail = "noreply@kuky.es";
        // day-start/day-end/lunch-break-* were removed when availability became teacher-managed
        // (V7): the public schedule is now derived from availability_rules/exceptions, not these.
        private int classDurationMinutes = 60;
        private int extendedClassDurationMinutes = 90;
        private int minLeadHours = 24;
        private int cancelCutoffHours = 24;
        private int bufferMinutes = 30;
        // Candidate start times are generated every slotStepMinutes within a window (e.g. 09:00,
        // 09:30, 10:00...), independent of class duration — this is what lets a booking end at
        // e.g. 10:30 and the *next* class start right there, instead of jumping to the next full
        // hour. Must divide evenly into 60 (5, 10, 15, 20, 30, 60) to stay aligned across hours.
        private int slotStepMinutes = 30;

        public String getTeacherTimezone() { return teacherTimezone; }
        public void setTeacherTimezone(String teacherTimezone) { this.teacherTimezone = teacherTimezone; }
        public String getTeacherEmail() { return teacherEmail; }
        public void setTeacherEmail(String teacherEmail) { this.teacherEmail = teacherEmail; }
        public int getClassDurationMinutes() { return classDurationMinutes; }
        public void setClassDurationMinutes(int classDurationMinutes) { this.classDurationMinutes = classDurationMinutes; }
        public int getExtendedClassDurationMinutes() { return extendedClassDurationMinutes; }
        public void setExtendedClassDurationMinutes(int extendedClassDurationMinutes) { this.extendedClassDurationMinutes = extendedClassDurationMinutes; }
        public int getMinLeadHours() { return minLeadHours; }
        public void setMinLeadHours(int minLeadHours) { this.minLeadHours = minLeadHours; }
        public int getCancelCutoffHours() { return cancelCutoffHours; }
        public void setCancelCutoffHours(int cancelCutoffHours) { this.cancelCutoffHours = cancelCutoffHours; }
        public int getBufferMinutes() { return bufferMinutes; }
        public void setBufferMinutes(int bufferMinutes) { this.bufferMinutes = bufferMinutes; }
        public int getSlotStepMinutes() { return slotStepMinutes; }
        public void setSlotStepMinutes(int slotStepMinutes) { this.slotStepMinutes = slotStepMinutes; }
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
