package com.apex.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.apex.ecommerce.dto.CheckoutRequest;
import com.apex.ecommerce.model.Order;
import com.apex.ecommerce.model.OrderItem;
import com.apex.ecommerce.model.Product;
import com.apex.ecommerce.repository.OrderRepository;
import com.apex.ecommerce.repository.ProductRepository;

import jakarta.transaction.Transactional;

@Service
public class OrderTransactionService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public OrderTransactionService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order executeStockAndOrderTransaction(List<CheckoutRequest.CartItem> items) {
        Order order = new Order();
        order.setStatus("PENDING");
        BigDecimal total = BigDecimal.ZERO;

        for (CheckoutRequest.CartItem item : items) {
            Product product = productRepository.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produk tidak ditemukan!"));

            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Stok produk " + product.getName() + " abis, lu telat!");
            }

            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice());
            order.getItems().add(orderItem);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    @Transactional
    public Order finalizeOrderStatus(Long orderId, boolean isSuccess) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order ghoib"));
        if (isSuccess) {
            order.setStatus("PAID");
        } else {
            order.setStatus("FAILED");
        }
        return orderRepository.save(order);
    }
}
