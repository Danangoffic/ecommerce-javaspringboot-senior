package com.apex.ecommerce.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductCreateRequest(
    @NotBlank @NotNull String name,
    @NotBlank @NotNull BigDecimal price,
    @NotBlank @NotNull Integer stock
) {}