package com.apex.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.apex.ecommerce.dto.ProductCreateRequest;
import com.apex.ecommerce.dto.ProductResponse;
import com.apex.ecommerce.dto.ProductUpdateRequest;
import com.apex.ecommerce.model.Product;
import com.apex.ecommerce.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product(1L, "Laptop Gaming", new BigDecimal("15000000"), 10, false);
    }

    @Test
    @DisplayName("getAllProducts mengembalikan halaman produk")
    void getAllProducts() {
        var pageable = PageRequest.of(0, 10);
        var productPage = new PageImpl<>(List.of(product), pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(productPage);

        Page<ProductResponse> result = productService.getAllProducts(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Laptop Gaming");
        assertThat(result.getContent().get(0).stockStatus()).isEqualTo("AVAILABLE");
        verify(productRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getProductById mengembalikan produk ketika ditemukan")
    void getProductById_found() {
        when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));

        var result = productService.getProductById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Laptop Gaming");
    }

    @Test
    @DisplayName("getProductById throw RuntimeException ketika produk tidak ditemukan")
    void getProductById_notFound() {
        when(productRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ghoib");
    }

    @Test
    @DisplayName("createProduct menyimpan dan mengembalikan produk baru")
    void createProduct() {
        var request = new ProductCreateRequest("Mouse", new BigDecimal("250000"), 50);
        var savedProduct = new Product(2L, "Mouse", new BigDecimal("250000"), 50, false);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        var result = productService.createProduct(request);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("Mouse");
        assertThat(result.stockStatus()).isEqualTo("AVAILABLE");

        verify(productRepository).save(productCaptor.capture());
        var captured = productCaptor.getValue();
        assertThat(captured.getName()).isEqualTo("Mouse");
        assertThat(captured.getPrice()).isEqualByComparingTo(new BigDecimal("250000"));
        assertThat(captured.getStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("updateProduct mengupdate dan mengembalikan produk")
    void updateProduct() {
        var request = new ProductUpdateRequest("Laptop Gaming Pro", new BigDecimal("18000000"), 5);
        when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = productService.updateProduct(1L, request);

        assertThat(result.name()).isEqualTo("Laptop Gaming Pro");
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("18000000"));
        assertThat(result.stock()).isEqualTo(5);
        assertThat(result.stockStatus()).isEqualTo("LIMITED_STOCK");
    }

    @Test
    @DisplayName("updateProduct throw RuntimeException ketika produk tidak ditemukan")
    void updateProduct_notFound() {
        var request = new ProductUpdateRequest("X", BigDecimal.ONE, 1);
        when(productRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ghoib");
    }

    @Test
    @DisplayName("deleteProduct menjalankan soft delete")
    void deleteProduct() {
        when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    @DisplayName("deleteProduct throw RuntimeException ketika produk tidak ditemukan")
    void deleteProduct_notFound() {
        when(productRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mau hapus apa");
    }

    @Test
    @DisplayName("stockStatus OUT_OF_STOCK untuk stok <= 0")
    void stockStatusOutOfStock() {
        var zeroStock = new Product(3L, "Barang Kosong", new BigDecimal("1000"), 0, false);
        when(productRepository.findActiveById(3L)).thenReturn(Optional.of(zeroStock));

        var result = productService.getProductById(3L);

        assertThat(result.stockStatus()).isEqualTo("OUT_OF_STOCK");
    }
}
