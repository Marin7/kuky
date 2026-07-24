package com.kuky.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.images")
public class ImageProperties {

    /** Writable directory for uploaded images (not the classpath seed folder). */
    private String storageDir = "./data/images";

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }
}
