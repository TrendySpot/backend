package com.spotz.domain.ticket;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReserveRequest {
    @NotNull
    private Long scheduleId;

    @Min(1)
    private int ticketCount;
}
