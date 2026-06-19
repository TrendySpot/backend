package com.spotz.domain.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AdminSpotRequest {
    @NotBlank private String title;
    private String description;
    @NotBlank private String spotType;
    @NotBlank private String area;
    @NotBlank private String address;
    @NotNull  private Double latitude;
    @NotNull  private Double longitude;
    @NotNull  private LocalDate startDate;
    @NotNull  private LocalDate endDate;
    private String imageUrl;
    private Integer price;
    private Integer totalTickets;
}
