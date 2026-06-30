package com.apex.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.apex.ecommerce.model.Order;
import com.apex.ecommerce.model.OrderItem;
import com.apex.ecommerce.model.Product;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Product productA;
    private Product productB;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        orderRepository.deleteAll();

        productA = productRepository.save(new Product(null, "Mouse", new BigDecimal("250000"), 50, false));
        productB = productRepository.save(new Product(null, "Keyboard", new BigDecimal("500000"), 30, false));
    }

    @Test
    @DisplayName("Menyimpan order dengan item-itemnya (cascade)")
    void saveOrderWithItems() {
        var order = new Order();
        order.setStatus("PENDING");
        order.setTotalAmount(new BigDecimal("1250000"));

        var item1 = new OrderItem(null, order, productA, 2, productA.getPrice());
        var item2 = new OrderItem(null, order, productB, 1, productB.getPrice());
        order.setItems(List.of(item1, item2));

        var saved = orderRepository.save(order);

        // Flush & clear persistence context agar query benar-benar dijalankan
        entityManager.flush();
        entityManager.clear();

        var found = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo("PENDING");
        assertThat(found.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1250000"));
        assertThat(found.getItems()).hasSize(2);
        assertThat(found.getItems().get(0).getProduct().getId()).isEqualTo(productA.getId());
        assertThat(found.getItems().get(1).getProduct().getId()).isEqualTo(productB.getId());
    }

    @Test
    @DisplayName("Update status order (PENDING -> PAID)")
    void updateOrderStatus() {
        var order = orderRepository.save(new Order(null, new BigDecimal("500000"), "PENDING", List.of()));

        entityManager.flush();
        entityManager.clear();

        var found = orderRepository.findById(order.getId()).orElseThrow();
        found.setStatus("PAID");
        orderRepository.save(found);

        entityManager.flush();
        entityManager.clear();

        var updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("Menyimpan order tanpa items (PENDING awal)")
    void saveOrderWithoutItems() {
        var order = new Order();
        order.setStatus("PENDING");
        order.setTotalAmount(BigDecimal.ZERO);

        var saved = orderRepository.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getItems()).isEmpty();
    }
}
