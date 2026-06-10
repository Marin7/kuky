package com.kuky.backend.resources.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Purchase {

    public enum ItemType { RESOURCE, BUNDLE }

    private UUID id;
    private UUID userId;
    private ItemType itemType;
    private UUID resourceId;
    private UUID bundleId;
    private int amountCents;
    private String currency;
    private String receiptReference;
    private String paymentProvider;
    private String paymentReference;
    private Instant purchasedAt;
    private List<String> grantedResourceSlugs;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public UUID getBundleId() { return bundleId; }
    public void setBundleId(UUID bundleId) { this.bundleId = bundleId; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getReceiptReference() { return receiptReference; }
    public void setReceiptReference(String receiptReference) { this.receiptReference = receiptReference; }
    public String getPaymentProvider() { return paymentProvider; }
    public void setPaymentProvider(String paymentProvider) { this.paymentProvider = paymentProvider; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public Instant getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(Instant purchasedAt) { this.purchasedAt = purchasedAt; }
    public List<String> getGrantedResourceSlugs() { return grantedResourceSlugs; }
    public void setGrantedResourceSlugs(List<String> grantedResourceSlugs) { this.grantedResourceSlugs = grantedResourceSlugs; }
}
