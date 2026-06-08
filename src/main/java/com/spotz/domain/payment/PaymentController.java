package com.spotz.domain.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/complete")
    public ResponseEntity<?> completePayment(@Valid @RequestBody PaymentVerifyRequest request) {
        try {
            log.info("결제 요청 수신 - ticketId: {}, portonePaymentId: {}, merchantUid: {}, amount: {}",
                    request.getTicketId(), request.getPortonePaymentId(),
                    request.getMerchantUid(), request.getAmount());

            PaymentResponse response = paymentService.verifyAndSavePayment(
                    request.getPortonePaymentId(),
                    request.getMerchantUid(),
                    request.getTicketId()
            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("결제 검증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("결제 처리 중 예외 발생", e);  // ← 전체 스택트레이스 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("결제 처리 중 서버 오류가 발생했습니다.");
        }
    }
}