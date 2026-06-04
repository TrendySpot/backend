package com.spotz.domain.payment;

import com.spotz.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verify(
            @AuthenticationPrincipal Long memberId,
            @RequestBody PaymentVerifyRequest req) {
        return ResponseEntity.ok(ApiResponse.of(paymentService.verifyAndSave(memberId, req)));
    }
}
