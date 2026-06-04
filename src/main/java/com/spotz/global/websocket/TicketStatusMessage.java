package com.spotz.global.websocket;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TicketStatusMessage {
    private Long scheduleId;
    private Long spotId;
    private LocalDate eventDate;
    private int totalTickets;
    private int remainedTickets;
}
