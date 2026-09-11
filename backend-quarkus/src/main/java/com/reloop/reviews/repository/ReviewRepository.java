package com.reloop.reviews.repository;

import com.reloop.common.jpa.ReloopRepository;
import com.reloop.reviews.domain.Review;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ReviewRepository implements ReloopRepository<Review, UUID> {

    public List<Review> findBySellerId(Long sellerId) {
        return list("sellerId", sellerId);
    }

    public Optional<Review> findByFulfillmentOrderId(UUID fulfillmentOrderId) {
        return find("fulfillmentOrderId", fulfillmentOrderId).firstResultOptional();
    }

    public Double averageRatingBySellerId(Long sellerId) {
        Double avg = getEntityManager()
                .createQuery("SELECT AVG(r.rating) FROM Review r WHERE r.sellerId = :sellerId", Double.class)
                .setParameter("sellerId", sellerId)
                .getSingleResult();
        return avg;
    }
}
