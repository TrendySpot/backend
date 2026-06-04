package com.spotz.global.oauth;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User oAuth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();

        String email;
        String nickname;

        if ("kakao".equals(registrationId)) {
            Map<?, ?> kakaoAccount = (Map<?, ?>) oAuth2User.getAttributes().get("kakao_account");
            Map<?, ?> profile = (Map<?, ?>) kakaoAccount.get("profile");
            email = (String) kakaoAccount.get("email");
            nickname = (String) profile.get("nickname");
        } else {
            Map<?, ?> response = (Map<?, ?>) oAuth2User.getAttributes().get("response");
            email = (String) response.get("email");
            nickname = (String) response.get("name");
        }

        final String finalEmail = email;
        final String finalNickname = nickname;
        final String provider = registrationId.toUpperCase();

        memberRepository.findByEmail(finalEmail).orElseGet(() ->
            memberRepository.save(Member.builder()
                .email(finalEmail)
                .password("OAUTH2_" + provider)
                .nickname(finalNickname + "_" + System.currentTimeMillis() % 10000)
                .provider(provider)
                .build())
        );

        return oAuth2User;
    }
}
