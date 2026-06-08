package com.spotz.domain.member;

import com.spotz.global.email.EmailService;
import com.spotz.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Transactional
    public void register(RegisterRequest req) {
        String verified = redisTemplate.opsForValue().get("email:verified:" + req.getEmail());
        if (!"true".equals(verified)) throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        if (memberRepository.existsByEmail(req.getEmail())) throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        if (memberRepository.existsByNickname(req.getNickname())) throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        memberRepository.save(Member.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(req.getNickname())
                .build());
        redisTemplate.delete("email:verified:" + req.getEmail());
    }

    public LoginResponse login(LoginRequest req) {
        Member member = memberRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(req.getPassword(), member.getPassword()))
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");

        String accessToken = jwtProvider.createAccessToken(member.getMemberId(), member.getEmail(), member.getRole());
        String refreshToken = jwtProvider.createRefreshToken(member.getMemberId());
        redisTemplate.opsForValue().set("refresh:" + member.getMemberId(), refreshToken, Duration.ofMillis(1209600000L));
        return new LoginResponse(accessToken, refreshToken, member.getNickname(), member.getRole().name(),
                member.getMemberId(), member.getEmail());
    }

    public TokenRefreshResponse refresh(String refreshToken) {
        if (!jwtProvider.isValid(refreshToken)) throw new IllegalStateException("유효하지 않은 리프레시 토큰입니다.");
        Long memberId = jwtProvider.getMemberId(refreshToken);
        String stored = redisTemplate.opsForValue().get("refresh:" + memberId);
        if (!refreshToken.equals(stored)) throw new IllegalStateException("리프레시 토큰이 일치하지 않습니다.");
        Member member = memberRepository.findById(memberId).orElseThrow();
        return new TokenRefreshResponse(jwtProvider.createAccessToken(member.getMemberId(), member.getEmail(), member.getRole()));
    }

    public void logout(Long memberId) {
        redisTemplate.delete("refresh:" + memberId);
    }

    public void sendEmailCode(String email) {
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        redisTemplate.opsForValue().set("email:code:" + email, code, Duration.ofMinutes(5));
        emailService.sendVerificationEmail(email, code);
    }

    public void verifyEmailCode(String email, String code) {
        String stored = redisTemplate.opsForValue().get("email:code:" + email);
        if (!code.equals(stored)) throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        redisTemplate.opsForValue().set("email:verified:" + email, "true", Duration.ofMinutes(30));
        redisTemplate.delete("email:code:" + email);
    }

    @Transactional
    public void updateProfile(Long memberId, UpdateProfileRequest req) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        if (req.getNickname() != null) member.setNickname(req.getNickname());
        if (req.getProfileImage() != null) member.setProfileImage(req.getProfileImage());
        if (req.getPassword() != null) member.setPassword(passwordEncoder.encode(req.getPassword()));
    }

    public FindEmailResponse findEmail(String nickname) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("해당 닉네임으로 가입된 계정이 없습니다."));
        String email = member.getEmail();
        int atIndex = email.indexOf("@");
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        String maskedLocal = local.length() <= 2
                ? local.charAt(0) + "**"
                : local.substring(0, 2) + "*".repeat(local.length() - 2);
        return new FindEmailResponse(maskedLocal + domain);
    }

    public void sendPasswordResetCode(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 가입된 계정이 없습니다."));
        if (!"LOCAL".equals(member.getProvider()))
            throw new IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        redisTemplate.opsForValue().set("pw:code:" + email, code, Duration.ofMinutes(5));
        emailService.sendPasswordResetEmail(email, code);
    }

    public void verifyPasswordResetCode(String email, String code) {
        String stored = redisTemplate.opsForValue().get("pw:code:" + email);
        if (!code.equals(stored)) throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        redisTemplate.opsForValue().set("pw:verified:" + email, "true", Duration.ofMinutes(10));
        redisTemplate.delete("pw:code:" + email);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String verified = redisTemplate.opsForValue().get("pw:verified:" + req.getEmail());
        if (!"true".equals(verified)) throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        Member member = memberRepository.findByEmail(req.getEmail()).orElseThrow();
        member.setPassword(passwordEncoder.encode(req.getNewPassword()));
        redisTemplate.delete("pw:verified:" + req.getEmail());
    }
}
