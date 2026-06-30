package com.apex.ecommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "products")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
// Mantra 1: Mengubah query DELETE menjadi UPDATE status is_deleted
@SQLDelete(sql = "UPDATE products SET is_deleted = true WHERE id = ?")
// Mantra 2: Otomatis memfilter produk yang belum dihapus di SEMUA query SELECT (termasuk pagination & findById)
@SQLRestriction("is_deleted = false")
public class Product {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}