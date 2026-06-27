package com.kuky.backend.learning.model;

import java.util.UUID;

/** An uploaded audio file backing a listening homework, stored as raw bytes. */
public class AudioFile {
    private UUID id;
    private String originalName;
    private String contentType;
    private int byteSize;
    private byte[] data;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public int getByteSize() { return byteSize; }
    public void setByteSize(int byteSize) { this.byteSize = byteSize; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
