package com.apex.ecommerce.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.apex.ecommerce.dto.CheckoutRequest;
import com.apex.ecommerce.dto.CheckoutResponse;
import com.apex.ecommerce.external.ExternalPaymentClient;

@Service
public class OrderService {
    private final OrderTransactionService orderTransactionService;
    private final ExternalPaymentClient paymentClient;

    public OrderService(OrderTransactionService orderTransactionService, ExternalPaymentClient paymentClient) {
        this.orderTransactionService = orderTransactionService;
        this.paymentClient = paymentClient;
    }

    // Method utama SENGAJA TIDAK pakai @Transactional global.
    // Biar koneksi DB HikariCP dilepas pas kita lagi hit External Payment API yang lambat.
    public CheckoutResponse checkout(CheckoutRequest request) {
        // SOLUSI DEADLOCK: Strict Ordering!
        // Urutkan ID produk dari yang terkecil sebelum locking.
        // Mencegah circular wait antar thread.
        List<CheckoutRequest.CartItem> sortedItems = request.getItems().stream()
                .sorted(Comparator.comparing(CheckoutRequest.CartItem::getProductId))
                .toList();

        // Step 1: Transaksi DB untuk stok & simpan order (via proxy, @Transactional jalan)
        var order = orderTransactionService.executeStockAndOrderTransaction(sortedItems);

        // Step 2: Hit External Payment API — OUTSIDE DB TRANSACTION
        // Wajib ada Read/Connect Timeout + Circuit Breaker (Resilience4j)
        boolean isPaymentSuccess = paymentClient.callPaymentGateway(order.getTotalAmount());

        // Step 3: Update status final order (via proxy, @Transactional jalan)
        var updatedOrder = orderTransactionService.finalizeOrderStatus(order.getId(), isPaymentSuccess);

        return CheckoutResponse.fromEntity(updatedOrder,
                isPaymentSuccess ? "Pembayaran sukses!" : "Pembayaran gagal, stok dikembalikan.");
    }
}
