package com.spotz.domain.spot;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "SPOT_SCHEDULE",
    uniqueConstraints = @UniqueConstraint(name = "uq_spot_date", columnNames = {"spot_id", "event_date"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SpotSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets;

    @Column(name = "remained_tickets", nullable = false)
    private Integer remainedTickets;

    public void decreaseTickets(int count) {
        if (this.remainedTickets < count) throw new IllegalStateException("잔여 티켓이 부족합니다.");
        this.remainedTickets -= count;
    }

    public void increaseTickets(int count) {
        this.remainedTickets = Math.min(this.remainedTickets + count, this.totalTickets);
    }
}
