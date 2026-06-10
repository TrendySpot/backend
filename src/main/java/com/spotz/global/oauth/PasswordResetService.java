package com.spotz.global.oauth;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

// [작성 06월 10일 16:33] 비밀번호 재설정 관련 핵심 로직(이메일 발송, Redis 토큰 관리, 패스워드 변경)을 처리하는 서비스 구현
@Service
@RequiredArgsConstructor
public class PasswordResetService {

	private final MemberRepository memberRepository;
	private final JavaMailSender mailSender;
	private final StringRedisTemplate redisTemplate;
	private final PasswordEncoder passwordEncoder;

	public void sendResetLink(String email) {
		if (!memberRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
		}

		String token = UUID.randomUUID().toString();
		redisTemplate.opsForValue().set(
				"RESET_TOKEN:" + token,
				email,
				5,
				TimeUnit.MINUTES
		);

		String resetLink = "http://localhost:3000/password-reset?token=" + token;
		sendEmail(email, resetLink);
	}

	private void sendEmail(String toEmail, String resetLink) {
		MimeMessage message = mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setTo(toEmail);
			helper.setSubject("[Trendy Spot] 비밀번호 재설정 링크 안내");

			String htmlContent = "<div style='margin:20px; padding:20px; border:1px solid #dddddd;'>"
					+ "<h2>비밀번호 재설정 안내</h2>"
					+ "<p>아래 링크를 클릭하여 비밀번호를 재설정해 주세요. 본 링크는 5분간만 유효합니다.</p>"
					+ "<a href='" + resetLink + "' style='display:inline-block; background:#10b981; color:white; padding:10px 20px; text-decoration:none; border-radius:5px;'>비밀번호 변경하기</a>"
					+ "</div>";

			helper.setText(htmlContent, true);
			mailSender.send(message);
		} catch (MessagingException e) {
			throw new RuntimeException("이메일 발송 중 오류가 발생했습니다.", e);
		}
	}

	public void resetPassword(String token, String newPassword) {
		String email = redisTemplate.opsForValue().get("RESET_TOKEN:" + token);
		if (email == null) {
			throw new IllegalArgumentException("만료되었거나 유효하지 않은 토큰입니다.");
		}

		Member member = memberRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

		member.updatePassword(passwordEncoder.encode(newPassword));
		memberRepository.save(member);

		redisTemplate.delete("RESET_TOKEN:" + token);
	}
}