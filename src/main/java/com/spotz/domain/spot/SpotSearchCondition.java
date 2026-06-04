package com.spotz.domain.spot;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SpotSearchCondition {
    private Spot.SpotType spotType;
    private Boolean free;
    private String area;
    private LocalDate date;
    private Boolean ongoing;
    private String keyword;
}
