package com.spotz.domain.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter // 가급적 요청 DTO도 @Data보다는 @Getter, @Setter 구체화가 안전합니다.
public class PaymentVerifyRequest {

    @NotNull(message = "티켓 ID는 필수입니다.")
    private Long ticketId;

    // impUid 대신 포트원 V2 명칭인 portonePaymentId로 변경합니다.
    @NotBlank(message = "포트원 결제 ID는 필수입니다.")
    private String portonePaymentId;

    @NotBlank(message = "가맹점 주문 번호는 필수입니다.")
    private String merchantUid;

    @NotNull(message = "결제 금액은 필수입니다.")
    private Integer amount;
}