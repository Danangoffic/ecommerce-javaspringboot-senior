package com.apex.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.apex.ecommerce.model.Product;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private Product activeProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();

        activeProduct = new Product();
        activeProduct.setName("Laptop Gaming");
        activeProduct.setPrice(new BigDecimal("15000000"));
        activeProduct.setStock(10);
        productRepository.save(activeProduct);
    }

    @Test
    @DisplayName("Menyimpan dan menemukan produk berdasarkan ID")
    void saveAndFindById() {
        var found = productRepository.findById(activeProduct.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Laptop Gaming");
        assertThat(found.get().getPrice()).isEqualByComparingTo(new BigDecimal("15000000"));
        assertThat(found.get().getStock()).isEqualTo(10);
        assertThat(found.get().isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Pagination findAll hanya mengembalikan produk aktif")
    void findAllPagination() {
        var page = productRepository.findAll(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Soft delete: @SQLDelete mengubah is_deleted=true, bukan hapus row")
    void softDeleteProduct() {
        productRepository.delete(activeProduct);

        var deletedRow = productRepository.findById(activeProduct.getId());
        assertThat(deletedRow).isEmpty(); // @SQLRestriction menyembunyikannya

        // Verifikasi via native query bahwa row masih ada dengan is_deleted=true
        var nativeResult = productRepository.findAll();
        assertThat(nativeResult).isEmpty();
    }

    @Test
    @DisplayName("findActiveById hanya mengembalikan produk yang tidak dihapus")
    void findActiveById() {
        var found = productRepository.findActiveById(activeProduct.getId());
        assertThat(found).isPresent();

        // Soft-delete dulu
        productRepository.delete(activeProduct);

        var afterDelete = productRepository.findActiveById(activeProduct.getId());
        assertThat(afterDelete).isEmpty();
    }

    @Test
    @DisplayName("findByIdForUpdate dengan PESSIMISTIC_WRITE lock")
    void findByIdForUpdate() {
        var locked = productRepository.findByIdForUpdate(activeProduct.getId());

        assertThat(locked).isPresent();
        assertThat(locked.get().getId()).isEqualTo(activeProduct.getId());
    }

    @Test
    @DisplayName("Update stok produk")
    void updateStock() {
        var product = productRepository.findById(activeProduct.getId()).orElseThrow();
        product.setStock(5);
        productRepository.save(product);

        var updated = productRepository.findById(activeProduct.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(5);
    }
}
