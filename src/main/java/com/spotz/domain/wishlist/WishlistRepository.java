package com.spotz.domain.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByMemberMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Wishlist> findByMemberMemberIdAndSpotSpotId(Long memberId, Long spotId);
}
