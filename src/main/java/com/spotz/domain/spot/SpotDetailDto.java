package com.spotz.domain.spot;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class SpotDetailDto {
    private Long spotId;
    private String title;
    private String description;
    private String spotType;
    private String area;
    private String address;
    private Double latitude;
    private Double longitude;
    private LocalDate startDate;
    private LocalDate endDate;
    private String imageUrl;
    private Integer price;
    private List<ScheduleDto> schedules;

    public static SpotDetailDto from(Spot s, List<SpotSchedule> schedules) {
        return SpotDetailDto.builder()
                .spotId(s.getSpotId()).title(s.getTitle()).description(s.getDescription())
                .spotType(s.getSpotType().name()).area(s.getArea()).address(s.getAddress())
                .latitude(s.getLatitude()).longitude(s.getLongitude())
                .startDate(s.getStartDate()).endDate(s.getEndDate())
                .imageUrl(s.getImageUrl()).price(s.getPrice())
                .schedules(schedules.stream().map(ScheduleDto::from).collect(Collectors.toList()))
                .build();
    }
}
