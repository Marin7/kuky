package com.kuky.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.presentation-files")
public class PresentationFileProperties {

    /** Writable directory for uploaded presentation PPTX files. */
    private String storageDir = "./data/presentation-files";

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }
}
