package com.kuky.backend.resources.model;

import java.time.Instant;
import java.util.UUID;

public class Resource {

    private UUID id;
    private String slug;
    private String title;
    private String description;
    private String level;
    private String category;
    private String pricing;
    private Integer priceCents;
    private String previewText;
    private UUID relatedResourceId;
    private boolean published;
    private int sortOrder;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPricing() { return pricing; }
    public void setPricing(String pricing) { this.pricing = pricing; }
    public Integer getPriceCents() { return priceCents; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
    public String getPreviewText() { return previewText; }
    public void setPreviewText(String previewText) { this.previewText = previewText; }
    public UUID getRelatedResourceId() { return relatedResourceId; }
    public void setRelatedResourceId(UUID relatedResourceId) { this.relatedResourceId = relatedResourceId; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
