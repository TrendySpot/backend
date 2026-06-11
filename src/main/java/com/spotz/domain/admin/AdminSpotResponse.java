package com.spotz.domain.admin;

import com.spotz.domain.spot.Spot;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminSpotResponse {
    private Long spotId;
    private String title;
    private String spotType;
    private String area;
    private String address;
    private Double latitude;
    private Double longitude;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer price;
    private String imageUrl;
    private String description;
    private LocalDateTime createdAt;

    public static AdminSpotResponse from(Spot s) {
        return AdminSpotResponse.builder()
                .spotId(s.getSpotId())
                .title(s.getTitle())
                .spotType(s.getSpotType().name())
                .area(s.getArea())
                .address(s.getAddress())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .price(s.getPrice())
                .imageUrl(s.getImageUrl())
                .description(s.getDescription())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
