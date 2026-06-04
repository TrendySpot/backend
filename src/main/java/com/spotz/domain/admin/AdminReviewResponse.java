package com.spotz.domain.admin;

import com.spotz.domain.review.Review;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminReviewResponse {
    private Long reviewId;
    private Long spotId;
    private String spotTitle;
    private Long memberId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;

    public static AdminReviewResponse from(Review r) {
        return AdminReviewResponse.builder()
                .reviewId(r.getReviewId())
                .spotId(r.getSpot().getSpotId())
                .spotTitle(r.getSpot().getTitle())
                .memberId(r.getMember().getMemberId())
                .nickname(r.getMember().getNickname())
                .content(r.getContent())
                .createdAt(r.getCreatedAt()).build();
    }
}
