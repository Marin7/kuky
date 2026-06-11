package com.kuky.backend.presentations.model;

import java.util.UUID;

public class Image {
    private UUID id;
    private String contentType;
    private int byteSize;
    private byte[] data;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public int getByteSize() { return byteSize; }
    public void setByteSize(int byteSize) { this.byteSize = byteSize; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
}
