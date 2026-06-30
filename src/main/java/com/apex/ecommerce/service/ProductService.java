package com.apex.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.apex.ecommerce.dto.ProductCreateRequest;
import com.apex.ecommerce.dto.ProductResponse;
import com.apex.ecommerce.dto.ProductUpdateRequest;
import com.apex.ecommerce.model.Product;
import com.apex.ecommerce.repository.ProductRepository;

import jakarta.transaction.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::fromEntity);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Produk ghoib atau sudah dihapus!"));
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());

        Product savedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        // Jika produk sudah di-soft-delete, findActiveById otomatis mengembalikan Optional.empty() berkat @SQLRestriction!
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Produk ghoib atau sudah dihapus!"));

        product.setName(request.name());
        product.setPrice(request.price());
        product.setStock(request.stock());

        Product updatedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new RuntimeException("Mau hapus apa? Produknya aja gak ada!"));
        
        // Panggil delete biasa. Di latar belakang, Hibernate akan mengubahnya menjadi query UPDATE status is_deleted=true
        productRepository.delete(product);
    }
}
