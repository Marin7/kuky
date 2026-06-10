package com.kuky.backend.resources.service;

import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.resources.dto.ReceiptLineItem;
import com.kuky.backend.resources.dto.ReceiptResponse;
import com.kuky.backend.resources.exception.ResourceNotFoundException;
import com.kuky.backend.resources.model.Purchase;
import com.kuky.backend.resources.repository.PurchaseRepository;
import com.kuky.backend.resources.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReceiptService {

    private final PurchaseRepository purchaseRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReceiptService(PurchaseRepository purchaseRepository,
                          ResourceRepository resourceRepository,
                          UserRepository userRepository) {
        this.purchaseRepository = purchaseRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public ReceiptResponse getReceipt(UUID purchaseId, UUID userId) {
        Purchase purchase = purchaseRepository.findByIdAndUserId(purchaseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compra no encontrada."));

        String buyerEmail = userRepository.findById(userId)
                .map(u -> u.getEmail())
                .orElse("");

        String itemTitle = "";
        List<ReceiptLineItem> lineItems;

        if (purchase.getItemType() == Purchase.ItemType.RESOURCE && purchase.getResourceId() != null) {
            var res = resourceRepository.findResourceById(purchase.getResourceId());
            itemTitle = res.map(r -> r.getTitle()).orElse("");
            lineItems = List.of(new ReceiptLineItem(itemTitle));
        } else if (purchase.getItemType() == Purchase.ItemType.BUNDLE && purchase.getBundleId() != null) {
            var bundle = resourceRepository.findBundleById(purchase.getBundleId());
            itemTitle = bundle.map(b -> b.getTitle()).orElse("");
            List<String> memberSlugs = resourceRepository.findBundleMemberSlugs(purchase.getBundleId());
            lineItems = memberSlugs.stream()
                    .map(slug -> resourceRepository.findResourceBySlug(slug)
                            .map(r -> new ReceiptLineItem(r.getTitle()))
                            .orElse(new ReceiptLineItem(slug)))
                    .toList();
        } else {
            lineItems = List.of();
        }

        return new ReceiptResponse(
                purchase.getReceiptReference(),
                purchase.getPurchasedAt(),
                buyerEmail,
                purchase.getItemType().name(),
                itemTitle,
                lineItems,
                purchase.getAmountCents(),
                "EUR",
                "Español con Paula"
        );
    }
}
