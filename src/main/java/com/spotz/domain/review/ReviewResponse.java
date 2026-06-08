package com.spotz.domain.review;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long reviewId;
    private Long memberId;
    private String nickname;
    private String profileImage;
    private String content;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review r) {
        return ReviewResponse.builder()
                .reviewId(r.getReviewId())
                .memberId(r.getMember().getMemberId())
                .nickname(r.getMember().getNickname())
                .profileImage(r.getMember().getProfileImage())
                .content(r.getContent())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
