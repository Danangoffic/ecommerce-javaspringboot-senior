package com.apex.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apex.ecommerce.dto.CheckoutRequest;
import com.apex.ecommerce.dto.CheckoutResponse;
import com.apex.ecommerce.service.OrderService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> doCheckout(@RequestBody CheckoutRequest request) {
        CheckoutResponse currentOrder = orderService.checkout(request);
        return ResponseEntity.ok(currentOrder);
    }
}
