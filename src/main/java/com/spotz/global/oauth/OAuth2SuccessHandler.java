package com.spotz.global.oauth;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
import com.spotz.global.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

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
        String email = extractEmail(oAuth2User);
        Member member = memberRepository.findByEmail(email).orElseThrow();

        String accessToken = jwtProvider.createAccessToken(member.getMemberId(), member.getEmail(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getMemberId());

        redisTemplate.opsForValue().set(
            "refresh:" + member.getMemberId(),
            refreshToken,
            Duration.ofMillis(1209600000L)
        );

        // 💡 프론트 개발 서버 주소 → 운영 시 https://spotz.co.kr 로 변경
        response.sendRedirect("http://localhost:3000/oauth2/callback?accessToken=" + accessToken + "&refreshToken=" + refreshToken);
    }

    private String extractEmail(DefaultOAuth2User user) {
        Map<String, Object> attrs = user.getAttributes();
        if (attrs.containsKey("kakao_account")) {
            return (String) ((Map<?, ?>) attrs.get("kakao_account")).get("email");
        }
        if (attrs.containsKey("response")) {
            return (String) ((Map<?, ?>) attrs.get("response")).get("email");
        }
        return (String) attrs.get("email");
    }
}
