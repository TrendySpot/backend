package com.spotz.domain.admin;

import com.spotz.domain.member.Member;
import com.spotz.domain.member.MemberRepository;
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
        memberRepository.deleteById(memberId);
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
