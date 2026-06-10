package com.kuky.backend.resources;

import com.kuky.backend.resources.exception.AlreadyOwnedException;
import com.kuky.backend.resources.exception.NotPurchasableException;
import com.kuky.backend.resources.exception.ResourceNotFoundException;
import com.kuky.backend.resources.model.Bundle;
import com.kuky.backend.resources.model.Resource;
import com.kuky.backend.resources.payment.PaymentProvider;
import com.kuky.backend.resources.repository.PurchaseRepository;
import com.kuky.backend.resources.repository.ResourceRepository;
import com.kuky.backend.resources.service.PurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    @Mock private ResourceRepository resourceRepository;
    @Mock private PurchaseRepository purchaseRepository;
    @Mock private PaymentProvider paymentProvider;

    private PurchaseService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID paidResourceId = UUID.randomUUID();
    private final UUID freeResourceId = UUID.randomUUID();
    private final UUID bundleId = UUID.randomUUID();
    private final UUID r1Id = UUID.randomUUID();
    private final UUID r2Id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PurchaseService(resourceRepository, purchaseRepository, paymentProvider);
    }

    @Test
    void purchase_resource_placeholderGrantsOwnership() {
        Resource paid = paidResource(paidResourceId, "paid-slug", 1500);
        when(resourceRepository.findResourceBySlug("paid-slug")).thenReturn(Optional.of(paid));
        when(resourceRepository.findOwnedResourceIds(userId)).thenReturn(Set.of());
        when(paymentProvider.authorize(1500, "EUR", "RESOURCE:paid-slug"))
                .thenReturn(new PaymentProvider.PaymentResult(true, "placeholder-ref"));
        when(purchaseRepository.insertPurchase(any())).thenAnswer(inv -> {
            var p = inv.getArgument(0, com.kuky.backend.resources.model.Purchase.class);
            p.setId(UUID.randomUUID());
            p.setReceiptReference("REC-2026-000001");
            p.setPurchasedAt(java.time.Instant.now());
            return p;
        });

        var response = service.purchase(userId, "RESOURCE", "paid-slug");

        assertThat(response.receiptReference()).isEqualTo("REC-2026-000001");
        assertThat(response.grantedResourceSlugs()).containsExactly("paid-slug");
        verify(purchaseRepository).insertEntitlements(eq(userId), eq(List.of(paidResourceId)), any());
    }

    @Test
    void purchase_resource_alreadyOwned_throws() {
        Resource paid = paidResource(paidResourceId, "paid-slug", 1500);
        when(resourceRepository.findResourceBySlug("paid-slug")).thenReturn(Optional.of(paid));
        when(resourceRepository.findOwnedResourceIds(userId)).thenReturn(Set.of(paidResourceId));

        assertThatThrownBy(() -> service.purchase(userId, "RESOURCE", "paid-slug"))
                .isInstanceOf(AlreadyOwnedException.class);
    }

    @Test
    void purchase_freeResource_throwsNotPurchasable() {
        Resource free = freeResource(freeResourceId, "free-slug");
        when(resourceRepository.findResourceBySlug("free-slug")).thenReturn(Optional.of(free));
        when(resourceRepository.findOwnedResourceIds(userId)).thenReturn(Set.of());

        assertThatThrownBy(() -> service.purchase(userId, "RESOURCE", "free-slug"))
                .isInstanceOf(NotPurchasableException.class);
    }

    @Test
    void purchase_resource_notFound_throws() {
        when(resourceRepository.findResourceBySlug("missing")).thenReturn(Optional.empty());
        when(resourceRepository.findOwnedResourceIds(userId)).thenReturn(Set.of());

        assertThatThrownBy(() -> service.purchase(userId, "RESOURCE", "missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void purchase_bundle_unlocksAllMembers_dedupsPreOwned() {
        Bundle bundle = bundle(bundleId, "bundle-slug", 3500);
        when(resourceRepository.findBundleBySlug("bundle-slug")).thenReturn(Optional.of(bundle));
        when(resourceRepository.findBundleMemberIds(bundleId)).thenReturn(List.of(r1Id, r2Id));
        when(resourceRepository.findBundleMemberSlugs(bundleId)).thenReturn(List.of("r1", "r2"));
        // user already owns r1
        when(resourceRepository.findOwnedResourceIds(userId)).thenReturn(Set.of(r1Id));
        when(paymentProvider.authorize(3500, "EUR", "BUNDLE:bundle-slug"))
                .thenReturn(new PaymentProvider.PaymentResult(true, "placeholder-bundle-ref"));
        when(purchaseRepository.insertPurchase(any())).thenAnswer(inv -> {
            var p = inv.getArgument(0, com.kuky.backend.resources.model.Purchase.class);
            p.setId(UUID.randomUUID());
            p.setReceiptReference("REC-2026-000002");
            p.setPurchasedAt(java.time.Instant.now());
            return p;
        });

        var response = service.purchase(userId, "BUNDLE", "bundle-slug");

        // Both r1 and r2 are passed to insertEntitlements (DB handles dedup via ON CONFLICT)
        verify(purchaseRepository).insertEntitlements(eq(userId), eq(List.of(r1Id, r2Id)), any());
        // Granted slugs only includes what was not previously owned
        assertThat(response.grantedResourceSlugs()).containsExactly("r2");
    }

    @Test
    void purchase_bundle_allOwned_throwsAlreadyOwned() {
        Bundle bundle = bundle(bundleId, "bundle-slug", 3500);
        when(resourceRepository.findBundleBySlug("bundle-slug")).thenReturn(Optional.of(bundle));
        when(resourceRepository.findBundleMemberIds(bundleId)).thenReturn(List.of(r1Id, r2Id));
        when(resourceRepository.findBundleMemberSlugs(bundleId)).thenReturn(List.of("r1", "r2"));
        when(resourceRepository.findOwnedResourceIds(userId)).thenReturn(Set.of(r1Id, r2Id));

        assertThatThrownBy(() -> service.purchase(userId, "BUNDLE", "bundle-slug"))
                .isInstanceOf(AlreadyOwnedException.class);
    }

    // ---- helpers ----

    private Resource paidResource(UUID id, String slug, int priceCents) {
        Resource r = new Resource();
        r.setId(id);
        r.setSlug(slug);
        r.setTitle("Título " + slug);
        r.setPricing("PAID");
        r.setPriceCents(priceCents);
        r.setPublished(true);
        return r;
    }

    private Resource freeResource(UUID id, String slug) {
        Resource r = new Resource();
        r.setId(id);
        r.setSlug(slug);
        r.setTitle("Título " + slug);
        r.setPricing("FREE");
        r.setPublished(true);
        return r;
    }

    private Bundle bundle(UUID id, String slug, int priceCents) {
        Bundle b = new Bundle();
        b.setId(id);
        b.setSlug(slug);
        b.setTitle("Bundle " + slug);
        b.setDescription("Descripción");
        b.setPriceCents(priceCents);
        b.setPublished(true);
        return b;
    }
}
