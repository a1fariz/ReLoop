package com.reloop.catalog.repository;

import com.reloop.catalog.domain.ProductModel;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProductModelRepository implements ReloopRepository<ProductModel, UUID> {

    public Optional<ProductModel> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional();
    }
}
