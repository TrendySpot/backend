package com.spotz.domain.admin;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
import com.spotz.domain.member.MemberService;
import com.spotz.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberRepository memberRepository;
    // [작성, 06월 12일 10:37] MemberService 의존성 주입 추가
    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminMemberResponse>>> getMembers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(memberRepository.findAll(pageable).map(AdminMemberResponse::from)));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> getMember(@PathVariable Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        return ResponseEntity.ok(ApiResponse.of(AdminMemberResponse.from(member)));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long memberId) {
        // [작성, 06월 12일 10:37] 기존 memberRepository.deleteById 지우고, 커스텀 삭제 비즈니스 로직(재고 복구, 결제/티켓/리뷰/찜 선삭제)을 수행하도록 교체
        memberService.deleteMember(memberId);
        return ResponseEntity.ok(ApiResponse.success("회원이 탈퇴 처리되었습니다."));
    }

    @PatchMapping("/{memberId}/role")
    public ResponseEntity<ApiResponse<Void>> updateRole(@PathVariable Long memberId,
                                                         @RequestParam Member.Role role) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.setRole(role);
        memberRepository.save(member);
        return ResponseEntity.ok(ApiResponse.success("권한이 변경되었습니다."));
    }
}
