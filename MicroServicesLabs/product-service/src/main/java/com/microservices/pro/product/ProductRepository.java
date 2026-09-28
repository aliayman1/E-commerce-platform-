package com.microservices.pro.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByPriceLessThan(BigDecimal price);

    @Query("SELECT new com.microservices.pro.product.ProductSummaryProjection(p.id, p.name, p.price, p.category) " +
            "FROM Product p WHERE p.id = :id")
    Optional<ProductSummaryProjection> findSummaryById(@Param("id") Long id);

    @Query("SELECT new com.microservices.pro.product.ProductSummaryProjection(p.id, p.name, p.price, p.category) " +
            "FROM Product p")
    List<ProductSummaryProjection> findAllSummaries();
}
