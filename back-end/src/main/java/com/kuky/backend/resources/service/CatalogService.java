package com.kuky.backend.resources.service;

import com.kuky.backend.resources.dto.*;
import com.kuky.backend.resources.exception.ResourceLockedException;
import com.kuky.backend.resources.exception.ResourceNotFoundException;
import com.kuky.backend.resources.model.Bundle;
import com.kuky.backend.resources.model.Resource;
import com.kuky.backend.resources.model.ResourceAsset;
import com.kuky.backend.resources.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CatalogService {

    private final ResourceRepository resourceRepository;

    public CatalogService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public CatalogResponse getCatalog(Optional<UUID> userId) {
        List<Resource> allResources = resourceRepository.findPublishedResources();
        Set<UUID> ownedIds = userId.map(resourceRepository::findOwnedResourceIds).orElse(Set.of());

        List<ResourceCardDto> freeResources = allResources.stream()
                .filter(r -> "FREE".equals(r.getPricing()))
                .map(r -> toCard(r, ownedIds))
                .toList();

        List<ResourceCardDto> paidResources = allResources.stream()
                .filter(r -> "PAID".equals(r.getPricing()))
                .map(r -> toCard(r, ownedIds))
                .toList();

        List<Bundle> bundles = resourceRepository.findPublishedBundles();
        List<BundleCardDto> bundleCards = bundles.stream()
                .map(b -> {
                    List<String> memberSlugs = resourceRepository.findBundleMemberSlugs(b.getId());
                    List<UUID> memberIds = resourceRepository.findBundleMemberIds(b.getId());
                    boolean allOwned = !memberIds.isEmpty() && ownedIds.containsAll(memberIds);
                    return new BundleCardDto(b.getSlug(), b.getTitle(), b.getDescription(),
                            b.getPriceCents(), memberSlugs, allOwned);
                })
                .toList();

        return new CatalogResponse("EUR", freeResources, paidResources, bundleCards);
    }

    public ResourceDetailDto getResource(String slug, Optional<UUID> userId) {
        Resource r = resourceRepository.findResourceBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado: " + slug));

        Set<UUID> ownedIds = userId.map(resourceRepository::findOwnedResourceIds).orElse(Set.of());
        boolean owned = "FREE".equals(r.getPricing()) || ownedIds.contains(r.getId());
        boolean locked = "PAID".equals(r.getPricing()) && !owned;

        String relatedSlug = resourceRepository.findRelatedResourceSlugById(r.getRelatedResourceId()).orElse(null);

        return new ResourceDetailDto(
                r.getSlug(), r.getTitle(), r.getDescription(), r.getLevel(), r.getCategory(),
                r.getPricing(), r.getPriceCents(), "EUR", r.getPreviewText(),
                owned, locked, relatedSlug
        );
    }

    public ContentResponse getContent(String slug, Optional<UUID> userId) {
        Resource r = resourceRepository.findResourceBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado: " + slug));

        boolean free = "FREE".equals(r.getPricing());
        if (!free) {
            boolean entitled = userId
                    .map(uid -> resourceRepository.hasEntitlement(uid, r.getId()))
                    .orElse(false);
            if (!entitled) {
                throw new ResourceLockedException("Este recurso requiere compra para acceder.");
            }
        }

        List<ResourceAsset> assets = resourceRepository.findAssetsByResourceId(r.getId());
        List<AssetDto> assetDtos = assets.stream()
                .map(a -> new AssetDto(a.getAssetType().name(), a.getLabel(), a.getLocator()))
                .toList();

        return new ContentResponse(r.getSlug(), assetDtos);
    }

    private ResourceCardDto toCard(Resource r, Set<UUID> ownedIds) {
        boolean owned = "FREE".equals(r.getPricing()) || ownedIds.contains(r.getId());
        boolean locked = "PAID".equals(r.getPricing()) && !owned;
        String relatedSlug = resourceRepository.findRelatedResourceSlugById(r.getRelatedResourceId()).orElse(null);
        return new ResourceCardDto(r.getSlug(), r.getTitle(), r.getDescription(), r.getLevel(),
                r.getCategory(), r.getPricing(), r.getPriceCents(), owned, locked, relatedSlug);
    }
}
