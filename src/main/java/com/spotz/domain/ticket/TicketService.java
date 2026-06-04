package com.spotz.domain.ticket;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
import com.spotz.domain.spot.SpotSchedule;
import com.spotz.domain.spot.SpotScheduleRepository;
import com.spotz.global.websocket.TicketStatusMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final SpotScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public TicketResponse reserve(Long memberId, ReserveRequest req) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        SpotSchedule schedule = scheduleRepository.findByIdWithLock(req.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        schedule.decreaseTickets(req.getTicketCount());

        Ticket ticket = Ticket.builder()
                .member(member).schedule(schedule)
                .ticketCount(req.getTicketCount()).build();
        ticketRepository.save(ticket);

        broadcastTicketStatus(schedule);
        return TicketResponse.from(ticket);
    }

    @Transactional
    public void cancel(Long memberId, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓입니다."));
        if (!ticket.getMember().getMemberId().equals(memberId))
            throw new SecurityException("본인의 예약만 취소할 수 있습니다.");
        if (ticket.getStatus() == Ticket.TicketStatus.CANCELED)
            throw new IllegalStateException("이미 취소된 예약입니다.");

        ticket.cancel();
        SpotSchedule schedule = scheduleRepository.findByIdWithLock(ticket.getSchedule().getScheduleId()).orElseThrow();
        schedule.increaseTickets(ticket.getTicketCount());
        broadcastTicketStatus(schedule);
    }

    public List<TicketResponse> getMyTickets(Long memberId) {
        return ticketRepository.findByMemberMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(TicketResponse::from).collect(Collectors.toList());
    }

    private void broadcastTicketStatus(SpotSchedule schedule) {
        messagingTemplate.convertAndSend(
            "/topic/tickets/" + schedule.getSpot().getSpotId(),
            TicketStatusMessage.builder()
                .scheduleId(schedule.getScheduleId())
                .spotId(schedule.getSpot().getSpotId())
                .eventDate(schedule.getEventDate())
                .totalTickets(schedule.getTotalTickets())
                .remainedTickets(schedule.getRemainedTickets())
                .build()
        );
    }
}
