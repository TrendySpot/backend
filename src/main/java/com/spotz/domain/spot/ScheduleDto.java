package com.spotz.domain.spot;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ScheduleDto {
    private Long scheduleId;
    private LocalDate eventDate;
    private Integer totalTickets;
    private Integer remainedTickets;

    public static ScheduleDto from(SpotSchedule sc) {
        return ScheduleDto.builder()
                .scheduleId(sc.getScheduleId()).eventDate(sc.getEventDate())
                .totalTickets(sc.getTotalTickets()).remainedTickets(sc.getRemainedTickets())
                .build();
    }
}
