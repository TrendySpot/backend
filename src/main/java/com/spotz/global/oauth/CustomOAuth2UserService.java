package com.spotz.global.oauth;

import com.spotz.domain.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();

        String email;
        String nickname;
        String providerId;

        /* * [수정일: 2026-06-05, 15:35]
         * 수정내용:
         * 1. 기존 네이버 중심의 하드코딩된 로직을 구글(Google) 로그인 지원을 위해 분기 처리함.
         * 2. DB 저장 및 조회의 일관성을 위해 직접 리포지토리를 호출하지 않고 MemberService를 사용하도록 변경함.
         */
        if ("kakao".equals(registrationId)) {
            Map<?, ?> attributes = oAuth2User.getAttributes();
            providerId = String.valueOf(attributes.get("id"));
            Map<?, ?> kakaoAccount = (Map<?, ?>) attributes.get("kakao_account");
            Map<?, ?> profile = (Map<?, ?>) kakaoAccount.get("profile");
            email = (String) kakaoAccount.get("email");
            nickname = (String) profile.get("nickname");
        } else { // Google
            providerId = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            nickname = oAuth2User.getAttribute("name");
        }

        memberService.findOrCreateSocialMember(email, registrationId.toUpperCase(), providerId, nickname);

        return oAuth2User;
    }
}