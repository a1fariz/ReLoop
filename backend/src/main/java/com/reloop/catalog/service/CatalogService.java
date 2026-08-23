package com.reloop.catalog.service;

import com.reloop.catalog.domain.ProductModel;
import com.reloop.catalog.dto.ProductModelDto;
import com.reloop.catalog.repository.ProductModelRepository;
import com.reloop.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {
    private final ProductModelRepository productModelRepository;

    public CatalogService(ProductModelRepository productModelRepository) {
        this.productModelRepository = productModelRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductModelDto> getAllModels() {
        return productModelRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductModelDto getModelBySlug(String slug) {
        return productModelRepository.findBySlug(slug)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException("Product model not found", "MODEL_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    private ProductModelDto toDto(ProductModel model) {
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
