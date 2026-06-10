package com.kuky.backend.resources.model;

import java.util.UUID;

public class ResourceAsset {

    public enum AssetType { FILE, EMBED }

    private UUID id;
    private UUID resourceId;
    private AssetType assetType;
    private String label;
    private String locator;
    private int sortOrder;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getLocator() { return locator; }
    public void setLocator(String locator) { this.locator = locator; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
