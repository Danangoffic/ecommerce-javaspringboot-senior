package com.apex.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.apex.ecommerce.model.Product;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // SOLUSI RACE CONDITION: Pakai Pessimistic Write Lock (SELECT ... FOR UPDATE)
    // Query ini bakal nge-lock row database sampai transaksi kelar, request lain dipaksa antre.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    // SOLUSI OUT OF MEMORY (OOM): Wajib hukumnya pakai Pagination!
    // Jangan pernah biarkan endpoint GET narik jutaan data kosongan tanpa batas size.
    Page<Product> findAll(Pageable pageable);

    @Query("""
        select p
        from Product p
        where p.id=:id
        and p.isDeleted=false
    """)
    Optional<Product> findActiveById(Long id);
}
