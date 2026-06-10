package com.spotz.domain.payment;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
import com.spotz.domain.spot.SpotSchedule;
import com.spotz.domain.spot.SpotScheduleRepository;
import com.spotz.domain.ticket.Ticket;
import com.spotz.domain.ticket.TicketRepository;
import com.spotz.global.websocket.TicketStatusMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final MemberRepository memberRepository;
    private final SpotScheduleRepository scheduleRepository;
    private final PortOneService portOneService;
    private final SimpMessagingTemplate messagingTemplate;

    public PaymentResponse verifyAndSavePayment(Long memberId, PaymentVerifyRequest req) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        SpotSchedule schedule = scheduleRepository.findByIdWithLock(req.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));

        Long spotPrice = schedule.getSpot().getPrice() != null ? schedule.getSpot().getPrice() : 0L;
        Long totalPrice = spotPrice * req.getTicketCount();

        // 무료 결제 처리
        if ("FREE".equals(req.getPortonePaymentId()) || totalPrice == 0L) {
            schedule.decreaseTickets(req.getTicketCount());
            Ticket ticket = Ticket.builder()
                    .member(member).schedule(schedule)
                    .ticketCount(req.getTicketCount())
                    .price(0L).build();
            ticketRepository.save(ticket);
            broadcastTicketStatus(schedule);

            Payment payment = Payment.builder()
                    .ticket(ticket)
                    .portonePaymentId("FREE")
                    .merchantUid(req.getMerchantUid() != null ? req.getMerchantUid() : "FREE_" + System.currentTimeMillis())
                    .amount(0L)
                    .status("FREE")
                    .build();
            return PaymentResponse.from(paymentRepository.save(payment));
        }

        // 포트원 V2 결제 검증
        PortOnePaymentResponse portOnePayment = portOneService.getPaymentInfo(req.getPortonePaymentId());

        if (!"PAID".equals(portOnePayment.status())) {
            throw new IllegalStateException("결제가 완료되지 않은 주문입니다. 상태: " + portOnePayment.status());
        }

        if (!totalPrice.equals((long) portOnePayment.amount().total())) {
            throw new IllegalStateException("결제 금액이 일치하지 않습니다. 위변조 가능성이 있습니다.");
        }

        // 결제 검증 완료 후 티켓 생성
        schedule.decreaseTickets(req.getTicketCount());
        Ticket ticket = Ticket.builder()
                .member(member).schedule(schedule)
                .ticketCount(req.getTicketCount())
                .price(totalPrice).build();
        ticketRepository.save(ticket);
        broadcastTicketStatus(schedule);

        Payment payment = Payment.builder()
                .ticket(ticket)
                .portonePaymentId(req.getPortonePaymentId())
                .merchantUid(req.getMerchantUid())
                .amount((long) portOnePayment.amount().total())
                .status("PAID")
                .build();

        return PaymentResponse.from(paymentRepository.save(payment));
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
