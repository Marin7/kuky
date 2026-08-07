package com.kuky.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.activity-instructions")
public class ActivityInstructionsProperties {

    /** Writable directory for activity instruction PDF files. */
    private String storageDir = "./data/activity-instructions";

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }
}
