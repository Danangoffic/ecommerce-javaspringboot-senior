package com.apex.ecommerce.dto;

import java.math.BigDecimal;

public record ProductUpdateRequest(
    String name,
    BigDecimal price,
    Integer stock
) {}