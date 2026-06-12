package com.spotz.domain.member;

import com.spotz.global.email.EmailService;
import com.spotz.global.jwt.JwtProvider;
// [작성, 06월 12일 10:16] 연관 데이터 수동 삭제 및 재고 복구를 위해 필요한 레포지토리 및 엔티티 임포트
import com.spotz.domain.ticket.Ticket;
import com.spotz.domain.ticket.TicketRepository;
import com.spotz.domain.review.ReviewRepository;
import com.spotz.domain.wishlist.WishlistRepository;
import com.spotz.domain.payment.PaymentRepository;
import com.spotz.domain.spot.SpotSchedule;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
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

    // [작성, 06월 12일 10:16] 연관 데이터 수동 제어를 위한 레포지토리 의존성 주입 추가
    private final TicketRepository ticketRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final PaymentRepository paymentRepository;

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

        // 1️⃣ 현재 비밀번호가 아예 안 넘어왔거나, DB에 암호화된 값과 다르면 컷트!
        if (req.getCurrentPassword() == null ||
                !passwordEncoder.matches(req.getCurrentPassword(), member.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 2️⃣ 현재 비밀번호가 맞았을 때만 아래 수정 로직들이 실행됨
        if (req.getNickname() != null) {
            member.setNickname(req.getNickname());
        }
        if (req.getProfileImage() != null) {
            member.setProfileImage(req.getProfileImage());
        }
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            member.setPassword(passwordEncoder.encode(req.getNewPassword()));
        }
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

    // [작성, 06월 12일 10:16] 명시적 자식 데이터 삭제 및 미래 일정 티켓 조건부 재고 복구 로직 구현
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 존재하지 않습니다."));

        // [작성, 06월 12일 10:16] TicketRepository의 회원 ID 기반 조회 메서드를 호출하여 티켓 목록을 가져옴
        List<Ticket> tickets = ticketRepository.findByMemberMemberIdOrderByCreatedAtDesc(memberId);
        for (Ticket ticket : tickets) {
            if (ticket.getStatus() == Ticket.TicketStatus.RESERVED) {
                SpotSchedule schedule = ticket.getSchedule();

                // [작성, 06월 12일 10:16] SpotSchedule의 eventDate가 오늘 이후인 경우에만 increaseTickets 메서드로 재고 복구
                if (schedule != null && schedule.getEventDate().isAfter(LocalDate.now())) {
                    schedule.increaseTickets(ticket.getTicketCount());
                }
            }
            // [작성, 06월 12일 10:16] 외래키 제약조건 위배를 방지하기 위해 티켓 ID에 연결된 결제 데이터 선삭제
            paymentRepository.deleteByTicketTicketId(ticket.getTicketId());
        }

        // [작성, 06월 12일 10:16] 조회된 회원의 모든 티켓 일괄 삭제
        ticketRepository.deleteAll(tickets);

        // [작성, 06월 12일 10:16] 회원 ID에 종속된 리뷰 및 찜 데이터 명시적 제거
        reviewRepository.deleteByMemberMemberId(memberId);
        wishlistRepository.deleteByMemberMemberId(memberId);

        // [작성, 06월 12일 10:16] 부모 테이블인 회원 데이터 최종 삭제
        memberRepository.delete(member);
    }
}