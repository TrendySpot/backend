package com.spotz.domain.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PortOneService {

    private final RestClient restClient;

    // application.yml 또는 properties에 등록한 secret 키를 가져옵니다.
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
                .header("Authorization", "PortOne " + apiSecret) // 인증 헤더 설정
                .retrieve()
                .body(PortOnePaymentResponse.class); // 응답을 DTO로 파싱
    }
}

// 포트원 API 응답을 매핑하기 위한 레코드(DTO)
record PortOnePaymentResponse(
        String id,
        String merchantUid,
        Amount amount,
        String status
) {
    record Amount(Long total) {}
}
