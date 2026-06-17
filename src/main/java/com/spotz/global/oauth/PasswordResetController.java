package com.spotz.global.oauth;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// [작성 06월 10일 16:33] 비밀번호 재설정 요청 및 완료 처리를 담당하는 REST 컨트롤러 구현
@RestController
@RequestMapping("/api/auth")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PasswordResetController {

	private final PasswordResetService passwordResetService;

	@PostMapping("/password-reset/request")
	public ResponseEntity<String> requestPasswordReset(@RequestParam("email") String email) {
		passwordResetService.sendResetLink(email);
		return ResponseEntity.ok("비밀번호 재설정 이메일이 발송되었습니다.");
	}

	@PostMapping("/password-reset/complete")
	public ResponseEntity<String> completePasswordReset(@RequestParam("token") String token,
	                                                    @RequestParam("newPassword") String newPassword) {
		passwordResetService.resetPassword(token, newPassword);
		return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
	}
}