package com.spotz.domain.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentVerifyRequest {

    // 결제 후 티켓 생성에 필요한 정보
    @NotNull(message = "스케줄 ID는 필수입니다.")
    private Long scheduleId;

    @Min(value = 1, message = "인원은 1명 이상이어야 합니다.")
    private int ticketCount;

    // 포트원 결제 정보
    @NotBlank(message = "포트원 결제 ID는 필수입니다.")
    private String portonePaymentId;

    private String merchantUid;

    @NotNull(message = "결제 금액은 필수입니다.")
    private Long amount;
}
