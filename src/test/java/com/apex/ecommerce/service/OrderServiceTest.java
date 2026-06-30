package com.apex.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.apex.ecommerce.dto.CheckoutRequest;
import com.apex.ecommerce.dto.CheckoutResponse;
import com.apex.ecommerce.external.ExternalPaymentClient;
import com.apex.ecommerce.model.Order;
import com.apex.ecommerce.model.OrderItem;
import com.apex.ecommerce.model.Product;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderTransactionService orderTransactionService;

    @Mock
    private ExternalPaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<List<CheckoutRequest.CartItem>> itemsCaptor;

    private CheckoutRequest request;
    private CheckoutRequest.CartItem itemA;
    private CheckoutRequest.CartItem itemB;
    private Order pendingOrder;

    @BeforeEach
    void setUp() {
        itemA = new CheckoutRequest.CartItem();
        itemA.setProductId(2L);
        itemA.setQuantity(1);

        itemB = new CheckoutRequest.CartItem();
        itemB.setProductId(1L);
        itemB.setQuantity(2);

        request = new CheckoutRequest();
        request.setItems(List.of(itemA, itemB)); // itemA (2) sebelum itemB (1) — tidak urut

        var product1 = new Product(1L, "Keyboard", new BigDecimal("500000"), 10, false);
        var product2 = new Product(2L, "Mouse", new BigDecimal("250000"), 20, false);

        var orderItem1 = new OrderItem(null, null, product1, 2, product1.getPrice());
        var orderItem2 = new OrderItem(null, null, product2, 1, product2.getPrice());

        pendingOrder = new Order(1L, new BigDecimal("1250000"), "PENDING",
                List.of(orderItem1, orderItem2));
        orderItem1.setOrder(pendingOrder);
        orderItem2.setOrder(pendingOrder);
    }

    @Test
    @DisplayName("checkout sukses: pembayaran berhasil, status PAID")
    void checkout_success() {
        when(orderTransactionService.executeStockAndOrderTransaction(anyList())).thenReturn(pendingOrder);
        when(paymentClient.callPaymentGateway(new BigDecimal("1250000"))).thenReturn(true);

        var paidOrder = new Order(1L, new BigDecimal("1250000"), "PAID", pendingOrder.getItems());
        when(orderTransactionService.finalizeOrderStatus(1L, true)).thenReturn(paidOrder);

        var response = orderService.checkout(request);

        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("PAID");
        assertThat(response.message()).isEqualTo("Pembayaran sukses!");
        assertThat(response.invoiceNumber()).startsWith("INV/");

        verify(orderTransactionService).executeStockAndOrderTransaction(itemsCaptor.capture());
        var capturedItems = itemsCaptor.getValue();
        assertThat(capturedItems).hasSize(2);
        assertThat(capturedItems.get(0).getProductId()).isEqualTo(1L); // sorted ascending
        assertThat(capturedItems.get(1).getProductId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("checkout gagal: pembayaran gagal, status FAILED")
    void checkout_paymentFailed() {
        when(orderTransactionService.executeStockAndOrderTransaction(anyList())).thenReturn(pendingOrder);
        when(paymentClient.callPaymentGateway(new BigDecimal("1250000"))).thenReturn(false);

        var failedOrder = new Order(1L, new BigDecimal("1250000"), "FAILED", pendingOrder.getItems());
        when(orderTransactionService.finalizeOrderStatus(1L, false)).thenReturn(failedOrder);

        var response = orderService.checkout(request);

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.message()).isEqualTo("Pembayaran gagal, stok dikembalikan.");
    }

    @Test
    @DisplayName("checkout: item diurutkan berdasarkan productId ascending (deadlock prevention)")
    void checkout_itemOrdering() {
        when(orderTransactionService.executeStockAndOrderTransaction(anyList())).thenReturn(pendingOrder);
        when(paymentClient.callPaymentGateway(new BigDecimal("1250000"))).thenReturn(true);
        when(orderTransactionService.finalizeOrderStatus(1L, true)).thenReturn(pendingOrder);

        orderService.checkout(request);

        verify(orderTransactionService).executeStockAndOrderTransaction(itemsCaptor.capture());
        var captured = itemsCaptor.getValue();
        assertThat(captured.get(0).getProductId()).isLessThan(captured.get(1).getProductId());
    }

    // Helper untuk Mockito varargs List
    @SuppressWarnings("unchecked")
    private static <T> List<T> anyList() {
        return org.mockito.ArgumentMatchers.anyList();
    }
}
