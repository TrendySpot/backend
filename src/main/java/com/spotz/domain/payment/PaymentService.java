package com.spotz.domain.payment;

import com.spotz.domain.ticket.Ticket;
import com.spotz.domain.ticket.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final PortOneService portOneService;

    /**
     * 결제 검증 및 저장
     */
    public PaymentResponse verifyAndSavePayment(String portonePaymentId, String merchantUid, Long ticketId) {

        // 1. 구매하려는 티켓 정보 조회 (가격 비교용)
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓입니다."));

        // 2. 포트원 API를 통해 실제 결제된 내역 서버 간 조회
        PortOnePaymentResponse portOnePayment = portOneService.getPaymentInfo(portonePaymentId);

        // 3. 결제 상태 검증
        if (!"PAID".equals(portOnePayment.status())) {
            throw new IllegalStateException("결제가 완료되지 않은 주문입니다. 상태: " + portOnePayment.status());
        }

        // 4. 금액 위변조 검증 (★가장 중요: 프론트엔드 조작 방지)
        // 만약 Ticket 엔티티의 가격 필드명이 다르면 수정해 주세요 (예: ticket.getAmount())
        if (!ticket.getPrice().equals(portOnePayment.amount().total())) {
            throw new IllegalStateException("결제 금액이 일치하지 않습니다. 위변조 가능성이 있습니다.");
        }

        // 5. 검증이 통과되면 엔티티 생성 및 DB 저장
        Payment payment = Payment.builder()
                .ticket(ticket)
                .portonePaymentId(portonePaymentId)
                .merchantUid(merchantUid)
                .amount(portOnePayment.amount().total())
                .status("PAID")
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // 6. 수정하신 DTO 포맷으로 결과 반환
        return PaymentResponse.from(savedPayment);
    }
}