package com.spotz.domain.member;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String nickname;
    private String role;
    private Long memberId;
    private String email;
    private String provider;  // ← 추가 (LOCAL, KAKAO, NAVER)
}
