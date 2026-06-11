package com.spotz.domain.ticket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TicketResponse {
    private Long ticketId;
    private Long scheduleId;
    private Long spotId;
    private LocalDate eventDate;
    private String spotTitle;
    private String imageUrl;
    private int ticketCount;
    private Long price;
    private String status;
    private String createdAt;

    public static TicketResponse from(Ticket t) {
        return TicketResponse.builder()
                .ticketId(t.getTicketId())
                .scheduleId(t.getSchedule().getScheduleId())
                .spotId(t.getSchedule().getSpot().getSpotId())
                .eventDate(t.getSchedule().getEventDate())
                .spotTitle(t.getSchedule().getSpot().getTitle())
                .imageUrl(t.getSchedule().getSpot().getImageUrl())
                .ticketCount(t.getTicketCount())
                .price(t.getPrice())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt().toString())
                .build();
    }
}
