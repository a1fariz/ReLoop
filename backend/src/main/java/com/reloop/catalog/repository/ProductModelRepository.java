package com.reloop.catalog.repository;

import com.reloop.catalog.domain.ProductModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductModelRepository extends JpaRepository<ProductModel, UUID> {
    Optional<ProductModel> findBySlug(String slug);
}
