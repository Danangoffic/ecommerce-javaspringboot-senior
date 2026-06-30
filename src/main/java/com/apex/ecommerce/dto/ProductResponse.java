package com.apex.ecommerce.dto;

import java.math.BigDecimal;

import com.apex.ecommerce.model.Product;

public record ProductResponse (
    Long id,
    String name,
    BigDecimal price,
    Integer stock,
    String stockStatus
) {
    // Static factory method buat mapping Entity ke DTO secara instan
    public static ProductResponse fromEntity(Product product) {
        String status = product.getStock() <= 0 ? "OUT_OF_STOCK" : 
                        product.getStock() <= 5 ? "LIMITED_STOCK" : "AVAILABLE";
                        
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                status
        );
    }
}