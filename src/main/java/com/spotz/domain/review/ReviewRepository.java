package com.spotz.domain.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findBySpotSpotIdOrderByCreatedAtDesc(Long spotId, Pageable pageable);

    // [작성, 06월 12일 10:16] 회원 탈퇴 처리를 위해 회원 ID로 리뷰 데이터를 삭제하는 메서드 추가
    void deleteByMemberMemberId(Long memberId);

    // [작성, 06월 12일 10:43] 스팟 삭제 시 관련 리뷰들을 일괄 삭제하기 위한 메서드 추가
    void deleteBySpotSpotId(Long spotId);
}