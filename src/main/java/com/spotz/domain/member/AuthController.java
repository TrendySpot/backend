package com.spotz.domain.member;

import com.spotz.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;

    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<Void>> sendEmailCode(@RequestParam String email) {
        memberService.sendEmailCode(email);
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송되었습니다."));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode(@RequestParam String email, @RequestParam String code) {
        memberService.verifyEmailCode(email, code);
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다."));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody @Valid RegisterRequest req) {
        memberService.register(req);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.of(memberService.login(req)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(ApiResponse.of(memberService.refresh(refreshToken)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long memberId) {
        memberService.logout(memberId);
        return ResponseEntity.ok(ApiResponse.success("로그아웃 되었습니다."));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateProfile(@AuthenticationPrincipal Long memberId,
                                                           @RequestBody UpdateProfileRequest req) {
        memberService.updateProfile(memberId, req);
        return ResponseEntity.ok(ApiResponse.success("프로필이 수정되었습니다."));
    }

    @GetMapping("/find-email")
    public ResponseEntity<ApiResponse<FindEmailResponse>> findEmail(@RequestParam String nickname) {
        return ResponseEntity.ok(ApiResponse.of(memberService.findEmail(nickname)));
    }

    @PostMapping("/password/send")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetCode(@RequestParam String email) {
        memberService.sendPasswordResetCode(email);
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 발송되었습니다."));
    }

    @PostMapping("/password/verify")
    public ResponseEntity<ApiResponse<Void>> verifyPasswordResetCode(@RequestParam String email, @RequestParam String code) {
        memberService.verifyPasswordResetCode(email, code);
        return ResponseEntity.ok(ApiResponse.success("인증이 완료되었습니다."));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        memberService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다."));
    }


    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(ApiResponse.of(memberService.checkNickname(nickname)));
    }

    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal Long memberId,
            @RequestParam String nickname) {

        memberService.updateNickname(memberId, nickname);

        return ResponseEntity.ok(
                ApiResponse.success("닉네임이 변경되었습니다.")
        );
    }


}
