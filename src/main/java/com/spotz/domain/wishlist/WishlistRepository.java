package com.spotz.domain.wishlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByMemberMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<Wishlist> findByMemberMemberIdAndSpotSpotId(Long memberId, Long spotId);

    // [작성, 06월 12일 10:47] 회원 탈퇴 처리를 위해 회원 ID로 찜 데이터를 삭제하는 메서드 추가
    void deleteByMemberMemberId(Long memberId);

    // [작성, 06월 12일 10:47] 스팟 삭제 시 관련 찜 내역들을 일괄 삭제하기 위한 메서드 추가
    void deleteBySpotSpotId(Long spotId);
}