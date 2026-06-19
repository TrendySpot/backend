package com.spotz.domain.member;

import com.spotz.global.email.EmailService;
import com.spotz.global.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtProvider jwtProvider;

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    EmailService emailService;

    @Mock
    ValueOperations<String, String> valueOperations;

    @InjectMocks
    MemberService memberService;

    @Test
    void 회원가입_성공() {

        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@test.com");
        req.setPassword("1234");
        req.setNickname("tester");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:verified:test@test.com"))
                .thenReturn("true");

        when(memberRepository.existsByEmail("test@test.com"))
                .thenReturn(false);

        when(memberRepository.existsByNickname("tester"))
                .thenReturn(false);

        when(passwordEncoder.encode("1234"))
                .thenReturn("encodedPassword");

        memberService.register(req);

        verify(memberRepository, times(1))
                .save(any(Member.class));

        verify(redisTemplate)
                .delete("email:verified:test@test.com");
    }

    @Test
    void 회원가입_이메일인증안됨() {

        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@test.com");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:verified:test@test.com"))
                .thenReturn(null);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> memberService.register(req)
        );

        assertEquals(
                "이메일 인증이 완료되지 않았습니다.",
                ex.getMessage()
        );
    }

    @Test
    void 로그인_성공() {

        Member member = Member.builder()
                .memberId(1L)
                .email("test@test.com")
                .password("encoded")
                .nickname("tester")
                .role(Member.Role.ROLE_USER)
                .build();

        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("1234");

        when(memberRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(member));

        when(passwordEncoder.matches("1234", "encoded"))
                .thenReturn(true);

        when(jwtProvider.createAccessToken(any(), any(), any()))
                .thenReturn("access-token");

        when(jwtProvider.createRefreshToken(1L))
                .thenReturn("refresh-token");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginResponse response = memberService.login(req);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());

        verify(valueOperations)
                .set(startsWith("refresh:"), eq("refresh-token"), any());
    }

    @Test
    void 로그인_비밀번호불일치() {

        Member member = Member.builder()
                .email("test@test.com")
                .password("encoded")
                .build();

        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("wrong");

        when(memberRepository.findByEmail("test@test.com"))
                .thenReturn(Optional.of(member));

        when(passwordEncoder.matches("wrong", "encoded"))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> memberService.login(req)
        );
    }

    @Test
    void 로그아웃_성공() {

        memberService.logout(1L);

        verify(redisTemplate)
                .delete("refresh:1");
    }
}