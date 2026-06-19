package com.spotz.domain.spot;

import com.spotz.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
public class SpotController {

    private final SpotService spotService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SpotSummaryDto>>> searchSpots(
            @ModelAttribute SpotSearchCondition cond,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(spotService.searchSpots(cond, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SpotDetailDto>> getSpotDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of(spotService.getSpotDetail(id)));
    }
}
