package com.spotz.domain.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByMemberMemberIdOrderByCreatedAtDesc(Long memberId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.schedule s JOIN FETCH s.spot WHERE t.ticketId = :id")
    Optional<Ticket> findWithScheduleAndSpot(@Param("id") Long id);

    // [작성, 06월 12일 10:44] 스케줄 ID로 티켓 목록을 조회하기 위한 메서드 추가
    List<Ticket> findByScheduleScheduleId(Long scheduleId);
}