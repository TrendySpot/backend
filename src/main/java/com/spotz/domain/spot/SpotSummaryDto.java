package com.spotz.domain.spot;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

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
    private Integer reservationRate;

    public static SpotSummaryDto from(Spot s, List<SpotSchedule> schedules) {
        int totalTickets = schedules.stream()
                .mapToInt(SpotSchedule::getTotalTickets)
                .sum();

        int remainedTickets = schedules.stream()
                .mapToInt(SpotSchedule::getRemainedTickets)
                .sum();

        int reservationRate = totalTickets == 0
                ? 0
                : (int) Math.round(((double) (totalTickets - remainedTickets) / totalTickets) * 100);

        return SpotSummaryDto.builder()
                .spotId(s.getSpotId())
                .title(s.getTitle())
                .spotType(s.getSpotType().name())
                .area(s.getArea())
                .address(s.getAddress())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .imageUrl(s.getImageUrl())
                .price(s.getPrice())
                .reservationRate(reservationRate)
                .build();
    }
}
