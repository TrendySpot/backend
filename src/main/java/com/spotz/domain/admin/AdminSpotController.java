package com.spotz.domain.admin;

import com.spotz.domain.spot.*;
import com.spotz.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/spots")
@RequiredArgsConstructor
public class AdminSpotController {

    private final SpotRepository spotRepository;
    private final SpotScheduleRepository scheduleRepository;
    // [작성, 06월 12일 10:47] 연쇄 삭제 비즈니스 로직 호출을 위해 SpotService 의존성 주입 추가
    private final SpotService spotService;


    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminSpotResponse>>> getSpots(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(spotRepository.findAll(pageable).map(AdminSpotResponse::from)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createSpot(@RequestBody AdminSpotRequest req) {
        Spot spot = spotRepository.save(Spot.builder()
                .title(req.getTitle()).description(req.getDescription())
                .spotType(Spot.SpotType.valueOf(req.getSpotType()))
                .area(req.getArea()).address(req.getAddress())
                .latitude(req.getLatitude()).longitude(req.getLongitude())
                .startDate(req.getStartDate()).endDate(req.getEndDate())
                .imageUrl(req.getImageUrl())
                .price(req.getPrice() != null ? req.getPrice() : 0)
                .build());

        LocalDate cursor = req.getStartDate();
        while (!cursor.isAfter(req.getEndDate())) {
            int tickets = req.getTotalTickets() != null ? req.getTotalTickets() : 100;
            scheduleRepository.save(SpotSchedule.builder()
                    .spot(spot).eventDate(cursor)
                    .totalTickets(tickets).remainedTickets(tickets).build());
            cursor = cursor.plusDays(1);
        }
        return ResponseEntity.ok(ApiResponse.success("스팟이 등록되었습니다."));
    }

    @PutMapping("/{spotId}")
    public ResponseEntity<ApiResponse<Void>> updateSpot(@PathVariable Long spotId,
                                                         @RequestBody AdminSpotRequest req) {
        Spot spot = spotRepository.findById(spotId).orElseThrow();
        spot.setTitle(req.getTitle()); spot.setDescription(req.getDescription());
        spot.setArea(req.getArea()); spot.setAddress(req.getAddress());
        spot.setLatitude(req.getLatitude()); spot.setLongitude(req.getLongitude());
        spot.setStartDate(req.getStartDate()); spot.setEndDate(req.getEndDate());
        spot.setImageUrl(req.getImageUrl());
        spot.setPrice(req.getPrice() != null ? req.getPrice() : 0);
        spotRepository.save(spot);
        return ResponseEntity.ok(ApiResponse.success("스팟이 수정되었습니다."));
    }

    @DeleteMapping("/{spotId}")
    public ResponseEntity<ApiResponse<Void>> deleteSpot(@PathVariable Long spotId) {
        // [수정, 06월 12일 10:47] 기존 spotRepository.deleteById 방식 대신, 모든 연관 관계를 안전하게 지워주는 spotService 로직으로 변경
        spotService.deleteSpot(spotId);
        return ResponseEntity.ok(ApiResponse.success("스팟이 삭제되었습니다."));
    }
}
