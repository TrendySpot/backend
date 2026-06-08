package com.spotz.domain.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private String portonePaymentId;
    private String merchantUid;
    private Long amount;
    private String status;
    private String paidAt;

    public static PaymentResponse from(Payment p) {
        String paidAtStr = (p.getPaidAt() != null) ? p.getPaidAt().toString() : java.time.LocalDateTime.now().toString();

        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .portonePaymentId(p.getPortonePaymentId())
                .merchantUid(p.getMerchantUid())
                .amount(p.getAmount())
                .status(p.getStatus())
                .paidAt(paidAtStr)
                .build();
    }
}
