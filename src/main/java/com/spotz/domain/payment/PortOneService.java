package com.spotz.domain.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PortOneService {

    private final RestClient restClient;

    @Value("${portone.api-secret}")
    private String apiSecret;

    public PortOneService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.portone.io")
                .build();
    }

    /**
     * 포트원 V2 단건 결제 조회 API 호출
     */
    public PortOnePaymentResponse getPaymentInfo(String portonePaymentId) {
        return restClient.get()
                .uri("/payments/{paymentId}", portonePaymentId)
                .header("Authorization", "PortOne " + apiSecret)
                .retrieve()
                .body(PortOnePaymentResponse.class);
    }
}

record PortOnePaymentResponse(
        String id,
        String merchantUid,
        Amount amount,
        String status
) {
    record Amount(Long total) {}
}
