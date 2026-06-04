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
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final RestTemplate restTemplate;

    @Value("${portone.imp-key}")
    private String impKey;

    @Value("${portone.api-secret}")
    private String apiSecret;

    @Transactional
    public PaymentResponse verifyAndSave(Long memberId, PaymentVerifyRequest req) {

        // 포트원 서버 검증 건너뜀 → 요청 금액 그대로 사용
        // 프론트 개발 완료 후 아래 주석 지울 것

        /*
        // 1. 포트원 액세스 토큰 발급
        String accessToken = getPortoneToken();

        // 2. 포트원에서 결제 정보 조회
        Map<String, Object> paymentData = getPaymentFromPortone(req.getImpUid(), accessToken);
        Integer paidAmount = (Integer) ((Map<?, ?>) paymentData.get("response")).get("amount");

        // 3. 금액 검증
        if (!paidAmount.equals(req.getAmount())) {
            throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
        }
        */

        // 개발용 임시 추가, 프론트에서 보내준 결제 금액을 검증 없이 그대로 믿고 사용합니다.
        Integer paidAmount = req.getAmount();

        // 4. 티켓 조회 및 본인 확인 (이 부분은 서비스 검증용이므로 그대로 둡니다)
        Ticket ticket = ticketRepository.findWithScheduleAndSpot(req.getTicketId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓입니다."));
        if (!ticket.getMember().getMemberId().equals(memberId))
            throw new SecurityException("본인의 티켓만 결제할 수 있습니다.");

        Payment payment = Payment.builder()
                .ticket(ticket).impUid(req.getImpUid())
                .merchantUid(req.getMerchantUid()).amount(paidAmount)
                .build();
        paymentRepository.save(payment);
        return PaymentResponse.from(payment);
    }

    private String getPortoneToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = Map.of("imp_key", impKey, "imp_secret", apiSecret);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "https://api.iamport.kr/users/getToken",
            new HttpEntity<>(body, headers), Map.class
        );
        return (String) ((Map<?, ?>) response.getBody().get("response")).get("access_token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPaymentFromPortone(String impUid, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> response = restTemplate.exchange(
            "https://api.iamport.kr/payments/" + impUid,
            HttpMethod.GET, new HttpEntity<>(headers), Map.class
        );
        return response.getBody();
    }
}
