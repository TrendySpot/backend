package com.spotz.domain.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Getter // 응답용 DTO는 데이터 변조를 막기 위해 @Data보다 @Getter를 권장합니다.
@Builder
@AllArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private String portonePaymentId; // 엔티티 변경에 맞춰 필드명 수정
    private String merchantUid;
    private Long amount;
    private String status;
    private String paidAt;

    public static PaymentResponse from(Payment p) {
        // paidAt이 null일 경우(DB 반영 전)를 대비해 현재 시간이나 빈 문자열 처리를 해줍니다.
        String paidAtStr = (p.getPaidAt() != null) ? p.getPaidAt().toString() : java.time.LocalDateTime.now().toString();

        return PaymentResponse.builder()
                .paymentId(p.getPaymentId())
                .portonePaymentId(p.getPortonePaymentId()) // 수정된 필드 반영
                .merchantUid(p.getMerchantUid())
                .amount(p.getAmount())
                .status(p.getStatus())
                .paidAt(paidAtStr) // 안전하게 변환된 문자열 대입
                .build();
    }
}