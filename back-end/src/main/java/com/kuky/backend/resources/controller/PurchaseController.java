package com.kuky.backend.resources.controller;

import com.kuky.backend.auth.repository.UserRepository;
import com.kuky.backend.resources.dto.MyPurchasesResponse;
import com.kuky.backend.resources.dto.PurchaseRequest;
import com.kuky.backend.resources.dto.PurchaseResponse;
import com.kuky.backend.resources.dto.ReceiptResponse;
import com.kuky.backend.resources.service.PurchaseService;
import com.kuky.backend.resources.service.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final ReceiptService receiptService;
    private final UserRepository userRepository;

    public PurchaseController(PurchaseService purchaseService,
                              ReceiptService receiptService,
                              UserRepository userRepository) {
        this.purchaseService = purchaseService;
        this.receiptService = receiptService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> purchase(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody PurchaseRequest request) {
        UUID userId = resolveUserId(email);
        PurchaseResponse response = purchaseService.purchase(userId, request.itemType(), request.slug());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<MyPurchasesResponse> listPurchases(
            @AuthenticationPrincipal String email) {
        UUID userId = resolveUserId(email);
        return ResponseEntity.ok(purchaseService.listPurchases(userId));
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(
            @AuthenticationPrincipal String email,
            @PathVariable UUID id) {
        UUID userId = resolveUserId(email);
        return ResponseEntity.ok(receiptService.getReceipt(id, userId));
    }

    private UUID resolveUserId(String email) {
        return userRepository.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."))
                .getId();
    }
}
