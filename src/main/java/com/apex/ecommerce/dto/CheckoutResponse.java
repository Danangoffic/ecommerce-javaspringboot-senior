package com.apex.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.apex.ecommerce.model.Order;

public record CheckoutResponse(
    Long orderId,
    String invoiceNumber,
    BigDecimal totalAmount,
    String status,
    String message,
    LocalDateTime createdAt,
    List<OrderItemDetail> items
) {
    // Nested Record khusus buat nge-wrap detail barang yang dibeli
    public record OrderItemDetail(
        Long productId,
        Integer quantity,
        BigDecimal priceAtPurchase,
        BigDecimal subTotal
    ) {}

    /**
     * SANGAT DIREKOMENDASIKAN: Bikin mapper langsung di dalam record.
     * Mengubah objek Order (Entity) menjadi CheckoutResponse (DTO) secara anggun.
     */
    public static CheckoutResponse fromEntity(Order order, String customMessage) {
        List<OrderItemDetail> detailItems = order.getItems().stream()
                .map(item -> new OrderItemDetail(
                        item.getProduct().getId(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();

        // Kita generate nomor invoice tiruan berdasarkan timestamp dan ID order
        String generatedInvoice = "INV/" + LocalDateTime.now().getYear() + "/" + order.getId();

        return new CheckoutResponse(
                order.getId(),
                generatedInvoice,
                order.getTotalAmount(),
                order.getStatus(),
                customMessage,
                LocalDateTime.now(), // Di production nyata, idealnya ambil dari database createdAt
                detailItems
        );
    }
}