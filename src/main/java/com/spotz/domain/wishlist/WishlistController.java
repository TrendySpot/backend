package com.spotz.domain.wishlist;

import com.spotz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishResponse>>> getMyWishlist(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.of(wishlistService.getMyWishlist(memberId)));
    }

    @PostMapping("/{spotId}")
    public ResponseEntity<ApiResponse<Void>> toggle(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long spotId) {
        wishlistService.toggle(memberId, spotId);
        return ResponseEntity.ok(ApiResponse.success("찜 상태가 변경되었습니다."));
    }
}
