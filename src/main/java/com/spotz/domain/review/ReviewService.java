package com.spotz.domain.review;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
import com.spotz.domain.spot.Spot;
import com.spotz.domain.spot.SpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final SpotRepository spotRepository;
    private final MemberRepository memberRepository;

    public Page<ReviewResponse> getReviews(Long spotId, Pageable pageable) {
        return reviewRepository.findBySpotSpotIdOrderByCreatedAtDesc(spotId, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional
    public ReviewResponse writeReview(Long memberId, Long spotId, ReviewRequest req) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Spot spot = spotRepository.findById(spotId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스팟입니다."));
        return ReviewResponse.from(reviewRepository.save(
            Review.builder()
                .member(member)
                .spot(spot)
                .content(req.getContent())
                .build()
        ));
    }

    @Transactional
    public void deleteReview(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후기입니다."));
        if (!review.getMember().getMemberId().equals(memberId))
            throw new SecurityException("본인의 후기만 삭제할 수 있습니다.");
        reviewRepository.delete(review);
    }

    @Transactional
    public ReviewResponse updateReview(Long memberId, Long reviewId, ReviewRequest req) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 후기입니다."));
        if (!review.getMember().getMemberId().equals(memberId))
            throw new SecurityException("본인의 후기만 수정할 수 있습니다.");

        review.setContent(req.getContent());
        review.setUpdatedAt(LocalDateTime.now());
        return ReviewResponse.from(review);
    }
}
