package com.spotz.domain.ticket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TicketResponse {
    private Long ticketId;
    private Long scheduleId;
    private LocalDate eventDate;
    private String spotTitle;
    private int ticketCount;
    private String status;
    private String createdAt;

    public static TicketResponse from(Ticket t) {
        return TicketResponse.builder()
                .ticketId(t.getTicketId())
                .scheduleId(t.getSchedule().getScheduleId())
                .eventDate(t.getSchedule().getEventDate())
                .spotTitle(t.getSchedule().getSpot().getTitle())
                .ticketCount(t.getTicketCount())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt().toString())
                .build();
    }
}
