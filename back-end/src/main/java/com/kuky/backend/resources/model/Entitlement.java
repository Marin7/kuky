package com.kuky.backend.resources.model;

import java.time.Instant;
import java.util.UUID;

public class Entitlement {

    private UUID id;
    private UUID userId;
    private UUID resourceId;
    private UUID sourcePurchaseId;
    private Instant grantedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public UUID getSourcePurchaseId() { return sourcePurchaseId; }
    public void setSourcePurchaseId(UUID sourcePurchaseId) { this.sourcePurchaseId = sourcePurchaseId; }
    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
}
