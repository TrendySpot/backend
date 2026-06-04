package com.spotz.domain.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentVerifyRequest {
    @NotNull  private Long ticketId;
    @NotBlank private String impUid;
    @NotBlank private String merchantUid;
    @NotNull  private Integer amount;
}
