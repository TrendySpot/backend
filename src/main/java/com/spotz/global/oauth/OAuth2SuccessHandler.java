package com.spotz.global.oauth;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
import com.spotz.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();

        /* * [수정일: 2026-06-05, 15:35]
         * 수정내용:
         * 1. 이메일만으로 회원을 찾을 경우 발생할 수 있는 소셜 계정 간 충돌을 방지하기 위해,
         * OAuth2AuthenticationToken을 사용하여 정확한 ProviderId와 RegistrationId로 회원을 조회하도록 수정.
         */
        String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String providerId = getProviderId(oAuth2User, registrationId);

        Member member = memberRepository.findByProviderAndProviderId(registrationId.toUpperCase(), providerId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        String accessToken = jwtProvider.createAccessToken(member.getMemberId(), member.getEmail(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getMemberId());

        redisTemplate.opsForValue().set("refresh:" + member.getMemberId(), refreshToken, Duration.ofMillis(1209600000L));

        response.sendRedirect("http://localhost:3000/oauth2/callback?accessToken=" + accessToken + "&refreshToken=" + refreshToken);
    }

    private String getProviderId(DefaultOAuth2User user, String registrationId) {
        if ("kakao".equals(registrationId)) return String.valueOf(user.getAttribute("id"));
        return user.getAttribute("sub"); // 구글
    }
}