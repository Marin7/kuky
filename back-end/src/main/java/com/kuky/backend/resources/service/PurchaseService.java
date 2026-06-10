package com.kuky.backend.resources.service;

import com.kuky.backend.resources.dto.MyPurchasesResponse;
import com.kuky.backend.resources.dto.PurchaseResponse;
import com.kuky.backend.resources.dto.PurchaseSummary;
import com.kuky.backend.resources.exception.AlreadyOwnedException;
import com.kuky.backend.resources.exception.NotPurchasableException;
import com.kuky.backend.resources.exception.ResourceNotFoundException;
import com.kuky.backend.resources.model.Bundle;
import com.kuky.backend.resources.model.Purchase;
import com.kuky.backend.resources.model.Resource;
import com.kuky.backend.resources.payment.PaymentProvider;
import com.kuky.backend.resources.repository.PurchaseRepository;
import com.kuky.backend.resources.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PurchaseService {

    private final ResourceRepository resourceRepository;
    private final PurchaseRepository purchaseRepository;
    private final PaymentProvider paymentProvider;

    public PurchaseService(ResourceRepository resourceRepository,
                           PurchaseRepository purchaseRepository,
                           PaymentProvider paymentProvider) {
        this.resourceRepository = resourceRepository;
        this.purchaseRepository = purchaseRepository;
        this.paymentProvider = paymentProvider;
    }

    @Transactional
    public PurchaseResponse purchase(UUID userId, String itemType, String slug) {
        Set<UUID> ownedIds = resourceRepository.findOwnedResourceIds(userId);

        if ("RESOURCE".equals(itemType)) {
            return purchaseResource(userId, slug, ownedIds);
        } else {
            return purchaseBundle(userId, slug, ownedIds);
        }
    }

    private PurchaseResponse purchaseResource(UUID userId, String slug, Set<UUID> ownedIds) {
        Resource resource = resourceRepository.findResourceBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado: " + slug));

        if ("FREE".equals(resource.getPricing())) {
            throw new NotPurchasableException("Este recurso es gratuito y no requiere compra.");
        }

        if (ownedIds.contains(resource.getId())) {
            throw new AlreadyOwnedException("Ya tienes acceso a este recurso.");
        }

        int amountCents = resource.getPriceCents() != null ? resource.getPriceCents() : 0;

        PaymentProvider.PaymentResult result = paymentProvider.authorize(
                amountCents, "EUR", "RESOURCE:" + slug);

        if (!result.approved()) {
            throw new RuntimeException("Pago no autorizado.");
        }

        Purchase purchase = new Purchase();
        purchase.setUserId(userId);
        purchase.setItemType(Purchase.ItemType.RESOURCE);
        purchase.setResourceId(resource.getId());
        purchase.setAmountCents(amountCents);
        purchase.setCurrency("EUR");
        purchase.setPaymentProvider("placeholder");
        purchase.setPaymentReference(result.providerReference());

        purchaseRepository.insertPurchase(purchase);
        purchaseRepository.insertEntitlements(userId, List.of(resource.getId()), purchase.getId());

        return new PurchaseResponse(
                purchase.getId().toString(),
                "RESOURCE",
                resource.getSlug(),
                resource.getTitle(),
                purchase.getAmountCents(),
                "EUR",
                purchase.getReceiptReference(),
                "placeholder",
                List.of(resource.getSlug()),
                purchase.getPurchasedAt()
        );
    }

    private PurchaseResponse purchaseBundle(UUID userId, String slug, Set<UUID> ownedIds) {
        Bundle bundle = resourceRepository.findBundleBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Pack no encontrado: " + slug));

        List<UUID> memberIds = resourceRepository.findBundleMemberIds(bundle.getId());
        List<String> memberSlugs = resourceRepository.findBundleMemberSlugs(bundle.getId());

        boolean allOwned = !memberIds.isEmpty() && ownedIds.containsAll(memberIds);
        if (allOwned) {
            throw new AlreadyOwnedException("Ya tienes acceso a todos los recursos de este pack.");
        }

        PaymentProvider.PaymentResult result = paymentProvider.authorize(
                bundle.getPriceCents(), "EUR", "BUNDLE:" + slug);

        if (!result.approved()) {
            throw new RuntimeException("Pago no autorizado.");
        }

        Purchase purchase = new Purchase();
        purchase.setUserId(userId);
        purchase.setItemType(Purchase.ItemType.BUNDLE);
        purchase.setBundleId(bundle.getId());
        purchase.setAmountCents(bundle.getPriceCents());
        purchase.setCurrency("EUR");
        purchase.setPaymentProvider("placeholder");
        purchase.setPaymentReference(result.providerReference());

        purchaseRepository.insertPurchase(purchase);
        // ON CONFLICT DO NOTHING handles already-owned members
        purchaseRepository.insertEntitlements(userId, memberIds, purchase.getId());

        // Newly granted = members not previously owned
        List<String> grantedSlugs = new ArrayList<>();
        for (int i = 0; i < memberIds.size(); i++) {
            if (!ownedIds.contains(memberIds.get(i))) {
                grantedSlugs.add(memberSlugs.get(i));
            }
        }

        return new PurchaseResponse(
                purchase.getId().toString(),
                "BUNDLE",
                bundle.getSlug(),
                bundle.getTitle(),
                purchase.getAmountCents(),
                "EUR",
                purchase.getReceiptReference(),
                "placeholder",
                grantedSlugs,
                purchase.getPurchasedAt()
        );
    }

    public MyPurchasesResponse listPurchases(UUID userId) {
        List<Purchase> purchases = purchaseRepository.findByUserId(userId);
        List<PurchaseSummary> summaries = purchases.stream()
                .map(this::toSummary)
                .toList();
        return new MyPurchasesResponse("EUR", summaries);
    }

    private PurchaseSummary toSummary(Purchase p) {
        String slug = "";
        String title = "";
        if (p.getItemType() == Purchase.ItemType.RESOURCE && p.getResourceId() != null) {
            var res = resourceRepository.findResourceById(p.getResourceId());
            if (res.isPresent()) {
                slug = res.get().getSlug();
                title = res.get().getTitle();
            }
        } else if (p.getItemType() == Purchase.ItemType.BUNDLE && p.getBundleId() != null) {
            var bundle = resourceRepository.findBundleById(p.getBundleId());
            if (bundle.isPresent()) {
                slug = bundle.get().getSlug();
                title = bundle.get().getTitle();
            }
        }
        return new PurchaseSummary(
                p.getId().toString(),
                p.getItemType().name(),
                slug,
                title,
                p.getAmountCents(),
                p.getReceiptReference(),
                p.getPurchasedAt(),
                List.of()
        );
    }
}
