package com.kuky.backend.resources.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Bundle {

    private UUID id;
    private String slug;
    private String title;
    private String description;
    private int priceCents;
    private boolean published;
    private int sortOrder;
    private Instant createdAt;
    private List<String> memberSlugs;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPriceCents() { return priceCents; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<String> getMemberSlugs() { return memberSlugs; }
    public void setMemberSlugs(List<String> memberSlugs) { this.memberSlugs = memberSlugs; }
}
