package com.apex.ecommerce.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
    private List<CartItem> items;

    @Getter
    @Setter
    public static class CartItem {
        private Long productId;
        private Integer quantity;
    }
}
