package com.spotz.domain.payment;

import com.spotz.domain.payment.PaymentResponse;
import com.spotz.domain.payment.PaymentService;
import com.spotz.domain.payment.PaymentVerifyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/complete")
    // 💡 PaymentRequest record 대신, 방금 만든 @Valid PaymentVerifyRequest를 매핑합니다.
    public ResponseEntity<?> completePayment(@Valid @RequestBody PaymentVerifyRequest request) {
        try {
            PaymentResponse response = paymentService.verifyAndSavePayment(
                    request.getPortonePaymentId(), // 수정된 Getter 메서드 호출
                    request.getMerchantUid(),
                    request.getTicketId()
            );
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("결제 처리 중 서버 오류가 발생했습니다.");
        }
    }
}