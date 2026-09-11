package com.reloop.catalog.service;

import com.reloop.catalog.domain.ProductModel;
import com.reloop.catalog.dto.ProductModelDto;
import com.reloop.catalog.repository.ProductModelRepository;
import com.reloop.common.exception.BusinessException;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class CatalogService {
    private final ProductModelRepository productModelRepository;

    @Inject
    public CatalogService(ProductModelRepository productModelRepository) {
        this.productModelRepository = productModelRepository;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ProductModelDto> getAllModels() {
        return productModelRepository.findAll().stream()
                .map(CatalogService::toDto)
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    @CacheResult(cacheName = "catalog-model-slug")
    public ProductModelDto getModelBySlug(String slug) {
        return productModelRepository.findBySlug(slug)
                .map(CatalogService::toDto)
                .orElseThrow(() -> new BusinessException("Product model not found", "MODEL_NOT_FOUND", 404));
    }

    public static ProductModelDto toDto(ProductModel model) {
        return new ProductModelDto(
                model.getId(),
                model.getCategoryId(),
                model.getBrand(),
                model.getModelName(),
                model.getSlug(),
                model.getMsrp(),
                model.getAnnualDepreciationRate(),
                model.getReleaseDate()
        );
    }
}
