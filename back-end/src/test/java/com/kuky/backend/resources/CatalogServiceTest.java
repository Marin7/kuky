package com.kuky.backend.resources;

import com.kuky.backend.resources.dto.CatalogResponse;
import com.kuky.backend.resources.dto.ContentResponse;
import com.kuky.backend.resources.dto.ResourceDetailDto;
import com.kuky.backend.resources.exception.ResourceLockedException;
import com.kuky.backend.resources.exception.ResourceNotFoundException;
import com.kuky.backend.resources.model.Bundle;
import com.kuky.backend.resources.model.Resource;
import com.kuky.backend.resources.model.ResourceAsset;
import com.kuky.backend.resources.repository.ResourceRepository;
import com.kuky.backend.resources.service.CatalogService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    private CatalogService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID freeResourceId = UUID.randomUUID();
    private final UUID paidResourceId = UUID.randomUUID();
    private final UUID bundleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CatalogService(resourceRepository);
    }

    @Test
    void getCatalog_splitsFreeAndPaidCorrectly() {
        Resource free = resource(freeResourceId, "free-slug", "FREE", null);
        Resource paid = resource(paidResourceId, "paid-slug", "PAID", 1500);
        when(resourceRepository.findPublishedResources()).thenReturn(List.of(free, paid));
        when(resourceRepository.findPublishedBundles()).thenReturn(List.of());

        CatalogResponse catalog = service.getCatalog(Optional.empty());

        assertThat(catalog.freeResources()).hasSize(1);
        assertThat(catalog.freeResources().get(0).slug()).isEqualTo("free-slug");
        assertThat(catalog.paidResources()).hasSize(1);
        assertThat(catalog.paidResources().get(0).slug()).isEqualTo("paid-slug");
        assertThat(catalog.currency()).isEqualTo("EUR");
    }

    @Test
    void getCatalog_freeResourceAlwaysUnlocked_anonymous() {
        Resource free = resource(freeResourceId, "free-slug", "FREE", null);
        when(resourceRepository.findPublishedResources()).thenReturn(List.of(free));
        when(resourceRepository.findPublishedBundles()).thenReturn(List.of());

        CatalogResponse catalog = service.getCatalog(Optional.empty());

        assertThat(catalog.freeResources().get(0).owned()).isTrue();
        assertThat(catalog.freeResources().get(0).locked()).isFalse();
    }

    @Test
    void getCatalog_paidResourceLockedForAnonymous() {
        Resource paid = resource(paidResourceId, "paid-slug", "PAID", 1500);
        when(resourceRepository.findPublishedResources()).thenReturn(List.of(paid));
        when(resourceRepository.findPublishedBundles()).thenReturn(List.of());

        CatalogResponse catalog = service.getCatalog(Optional.empty());

        assertThat(catalog.paidResources().get(0).owned()).isFalse();
        assertThat(catalog.paidResources().get(0).locked()).isTrue();
    }

    @Test
    void getCatalog_paidResourceUnlockedWhenEntitled() {
        Resource paid = resource(paidResourceId, "paid-slug", "PAID", 1500);
        when(resourceRepository.findPublishedResources()).thenReturn(List.of(paid));
        when(resourceRepository.findPublishedBundles()).thenReturn(List.of());
        when(resourceRepository.findOwnedResourceIds(userId)).thenReturn(Set.of(paidResourceId));

        CatalogResponse catalog = service.getCatalog(Optional.of(userId));

        assertThat(catalog.paidResources().get(0).owned()).isTrue();
        assertThat(catalog.paidResources().get(0).locked()).isFalse();
    }

    @Test
    void getCatalog_bundleOwnedOnlyWhenAllMembersOwned() {
        Resource r1 = resource(UUID.randomUUID(), "r1", "PAID", 1500);
        Resource r2 = resource(UUID.randomUUID(), "r2", "PAID", 2500);
        Bundle bundle = bundle(bundleId, "bundle-slug");
        when(resourceRepository.findPublishedResources()).thenReturn(List.of(r1, r2));
        when(resourceRepository.findPublishedBundles()).thenReturn(List.of(bundle));
        when(resourceRepository.findBundleMemberSlugs(bundleId)).thenReturn(List.of("r1", "r2"));
        when(resourceRepository.findBundleMemberIds(bundleId)).thenReturn(List.of(r1.getId(), r2.getId()));
        // user owns only r1
        when(resourceRepository.findOwnedResourceIds(userId)).thenReturn(Set.of(r1.getId()));

        CatalogResponse catalog = service.getCatalog(Optional.of(userId));

        assertThat(catalog.bundles().get(0).owned()).isFalse();
    }

    @Test
    void getCatalog_bundleOwnedWhenAllMembersOwned() {
        Resource r1 = resource(UUID.randomUUID(), "r1", "PAID", 1500);
        Resource r2 = resource(UUID.randomUUID(), "r2", "PAID", 2500);
        Bundle bundle = bundle(bundleId, "bundle-slug");
        when(resourceRepository.findPublishedResources()).thenReturn(List.of(r1, r2));
        when(resourceRepository.findPublishedBundles()).thenReturn(List.of(bundle));
        when(resourceRepository.findBundleMemberSlugs(bundleId)).thenReturn(List.of("r1", "r2"));
        when(resourceRepository.findBundleMemberIds(bundleId)).thenReturn(List.of(r1.getId(), r2.getId()));
        when(resourceRepository.findOwnedResourceIds(userId)).thenReturn(Set.of(r1.getId(), r2.getId()));

        CatalogResponse catalog = service.getCatalog(Optional.of(userId));

        assertThat(catalog.bundles().get(0).owned()).isTrue();
    }

    @Test
    void getResource_throwsNotFoundForMissingSlug() {
        when(resourceRepository.findResourceBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getResource("missing", Optional.empty()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getContent_freeResourceAccessibleWithoutAuth() {
        Resource free = resource(freeResourceId, "free-slug", "FREE", null);
        when(resourceRepository.findResourceBySlug("free-slug")).thenReturn(Optional.of(free));
        when(resourceRepository.findAssetsByResourceId(freeResourceId)).thenReturn(List.of(
                asset(freeResourceId, "FILE", "Ficha", "https://example.com/file.pdf")
        ));

        ContentResponse content = service.getContent("free-slug", Optional.empty());

        assertThat(content.assets()).hasSize(1);
        assertThat(content.assets().get(0).label()).isEqualTo("Ficha");
    }

    @Test
    void getContent_paidResourceBlockedForAnonymous() {
        Resource paid = resource(paidResourceId, "paid-slug", "PAID", 1500);
        when(resourceRepository.findResourceBySlug("paid-slug")).thenReturn(Optional.of(paid));

        assertThatThrownBy(() -> service.getContent("paid-slug", Optional.empty()))
                .isInstanceOf(ResourceLockedException.class);
    }

    @Test
    void getContent_paidResourceAccessibleWhenEntitled() {
        Resource paid = resource(paidResourceId, "paid-slug", "PAID", 1500);
        when(resourceRepository.findResourceBySlug("paid-slug")).thenReturn(Optional.of(paid));
        when(resourceRepository.hasEntitlement(userId, paidResourceId)).thenReturn(true);
        when(resourceRepository.findAssetsByResourceId(paidResourceId)).thenReturn(List.of());

        ContentResponse content = service.getContent("paid-slug", Optional.of(userId));

        assertThat(content.slug()).isEqualTo("paid-slug");
    }

    // ---- helpers ----

    private Resource resource(UUID id, String slug, String pricing, Integer priceCents) {
        Resource r = new Resource();
        r.setId(id);
        r.setSlug(slug);
        r.setTitle("Título " + slug);
        r.setDescription("Descripción");
        r.setPricing(pricing);
        r.setPriceCents(priceCents);
        r.setPublished(true);
        return r;
    }

    private Bundle bundle(UUID id, String slug) {
        Bundle b = new Bundle();
        b.setId(id);
        b.setSlug(slug);
        b.setTitle("Bundle " + slug);
        b.setDescription("Descripción bundle");
        b.setPriceCents(3500);
        b.setPublished(true);
        return b;
    }

    private ResourceAsset asset(UUID resourceId, String type, String label, String locator) {
        ResourceAsset a = new ResourceAsset();
        a.setId(UUID.randomUUID());
        a.setResourceId(resourceId);
        a.setAssetType(ResourceAsset.AssetType.valueOf(type));
        a.setLabel(label);
        a.setLocator(locator);
        return a;
    }
}
