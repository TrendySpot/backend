package com.spotz.domain.review;

import com.spotz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spots/{spotId}/reviews")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getReviews(
            @PathVariable Long spotId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(reviewService.getReviews(spotId, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> write(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long spotId,
            @RequestBody ReviewRequest req) {
        return ResponseEntity.ok(ApiResponse.of(reviewService.writeReview(memberId, spotId, req)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long reviewId) {
        reviewService.deleteReview(memberId, reviewId);
        return ResponseEntity.ok(ApiResponse.success("후기가 삭제되었습니다."));
    }

    @PatchMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long reviewId,
            @RequestBody ReviewRequest req) {
        return ResponseEntity.ok(ApiResponse.of(reviewService.updateReview(memberId, reviewId, req)));
    }
}
