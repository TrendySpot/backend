package com.spotz.domain.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private String impUid;
    private String merchantUid;
    private Integer amount;
    private String status;
    private String paidAt;

    public static PaymentResponse from(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getPaymentId()).impUid(p.getImpUid())
                .merchantUid(p.getMerchantUid()).amount(p.getAmount())
                .status(p.getStatus()).paidAt(p.getPaidAt().toString())
                .build();
    }
}
