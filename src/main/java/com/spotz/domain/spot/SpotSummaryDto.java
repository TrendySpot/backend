package com.spotz.domain.spot;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SpotSummaryDto {
    private Long spotId;
    private String title;
    private String spotType;
    private String area;
    private String address;
    private LocalDate startDate;
    private LocalDate endDate;
    private String imageUrl;
    private Integer price;

    public static SpotSummaryDto from(Spot s) {
        return SpotSummaryDto.builder()
                .spotId(s.getSpotId()).title(s.getTitle())
                .spotType(s.getSpotType().name()).area(s.getArea())
                .address(s.getAddress()).startDate(s.getStartDate())
                .endDate(s.getEndDate()).imageUrl(s.getImageUrl())
                .price(s.getPrice()).build();
    }
}
