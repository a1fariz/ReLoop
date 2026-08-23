package com.reloop.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "product_models")
public class ProductModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Integer categoryId;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String modelName;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal msrp;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal annualDepreciationRate = new BigDecimal("0.150");

    @Column(nullable = false)
    private LocalDate releaseDate;

    public ProductModel() {}

    public ProductModel(Integer categoryId, String brand, String modelName, String slug, BigDecimal msrp, BigDecimal annualDepreciationRate, LocalDate releaseDate) {
        this.categoryId = categoryId;
        this.brand = brand;
        this.modelName = modelName;
        this.slug = slug;
        this.msrp = msrp;
        this.annualDepreciationRate = annualDepreciationRate;
        this.releaseDate = releaseDate;
    }

    public UUID getId() { return id; }
    public Integer getCategoryId() { return categoryId; }
    public String getBrand() { return brand; }
    public String getModelName() { return modelName; }
    public String getSlug() { return slug; }
    public BigDecimal getMsrp() { return msrp; }
    public BigDecimal getAnnualDepreciationRate() { return annualDepreciationRate; }
    public LocalDate getReleaseDate() { return releaseDate; }
}
