package com.spotz.domain.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findBySpotSpotIdOrderByCreatedAtDesc(Long spotId, Pageable pageable);
}
