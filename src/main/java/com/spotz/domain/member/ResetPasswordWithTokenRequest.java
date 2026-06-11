package com.spotz.domain.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// [작성 6월 10일 15:35] 토큰 기반 비밀번호 재설정 요청을 바인딩하기 위한 신규 DTO 클래스 작성
@Getter
@NoArgsConstructor
public class ResetPasswordWithTokenRequest {

	@NotBlank(message = "인증 토큰은 필수입니다.")
	private String token;

	@NotBlank(message = "새로운 비밀번호를 입력해주세요.")
	@Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
	private String newPassword;
}