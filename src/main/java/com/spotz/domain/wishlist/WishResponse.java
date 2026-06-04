package com.spotz.domain.wishlist;

import com.spotz.domain.spot.Spot;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class WishResponse {
    private Long wishId;
    private Long spotId;
    private String title;
    private String spotType;
    private String area;
    private LocalDate startDate;
    private LocalDate endDate;
    private String imageUrl;
    private Integer price;

    public static WishResponse from(Wishlist w) {
        Spot s = w.getSpot();
        return WishResponse.builder()
                .wishId(w.getWishId()).spotId(s.getSpotId())
                .title(s.getTitle()).spotType(s.getSpotType().name())
                .area(s.getArea()).startDate(s.getStartDate())
                .endDate(s.getEndDate()).imageUrl(s.getImageUrl())
                .price(s.getPrice()).build();
    }
}
