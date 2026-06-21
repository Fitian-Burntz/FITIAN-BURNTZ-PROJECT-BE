package com.fitian.burntz.domain.membership.docs;

import com.fitian.burntz.domain.membership.v2.dto.MembershipHoldIdentifierRequest;
import com.fitian.burntz.domain.membership.v2.dto.MembershipHoldRequest;
import com.fitian.burntz.domain.membership.v2.dto.MembershipHoldResponse;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "멤버십 홀딩 API (v2)", description = "멤버십 홀딩 신청, 종료, 롤백, 이력 조회를 수행합니다.")
public interface MembershipHoldDocs {

    @Operation(summary = "홀딩 신청 (회원/운영자)",
            description = """
                    멤버십 홀딩을 신청합니다.
                    - holdStartDate가 오늘이면 즉시 ACTIVE, 미래 날짜면 SCHEDULED 상태로 생성됩니다.
                    - ACTIVE로 전환 시 홀딩 기간 내 수업 신청이 자동 취소됩니다.
                    - 기존 SCHEDULED/ACTIVE 홀딩과 날짜가 겹치면 거부됩니다.
                    - 신청 시 박스 운영자(MANAGER/OWNER)에게 푸시 알림이 전송됩니다.
                    - reason(사유)은 선택 입력입니다.
                    """)
    ResponseEntity<ApiResponse<MembershipHoldResponse>> createHold(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @Parameter(description = "대상 회원 PK") @PathVariable Long memberPk,
            @Valid @RequestBody MembershipHoldRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "홀딩 조기 종료 (회원/운영자)",
            description = """
                    진행 중인 홀딩을 조기에 종료합니다.
                    - ACTIVE 홀딩: 오늘 날짜 기준으로 멤버십 만료일을 재계산합니다.
                      (새 만료일 = 오늘 + (원래 만료일 - 홀딩 시작일))
                    - SCHEDULED 홀딩: 멤버십 상태 변경 없이 취소됩니다.
                    - COMPLETED/CANCELLED 상태는 종료할 수 없습니다.
                    """)
    ApiResponse<Void> endHold(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @Parameter(description = "대상 회원 PK") @PathVariable Long memberPk,
            @Valid @RequestBody MembershipHoldIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "홀딩 롤백/취소 (운영자 전용)",
            description = """
                    회원이 실수로 신청한 홀딩을 없었던 것으로 되돌립니다.
                    - ACTIVE 홀딩: 멤버십 만료일을 홀딩 이전 날짜로 완전 복원합니다.
                    - SCHEDULED 홀딩: 멤버십 상태 변경 없이 취소됩니다.
                    - COMPLETED/CANCELLED 상태는 롤백할 수 없습니다.
                    - MANAGER/OWNER만 호출 가능합니다.
                    """)
    ApiResponse<Void> cancelHold(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @Parameter(description = "대상 회원 PK") @PathVariable Long memberPk,
            @Valid @RequestBody MembershipHoldIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "홀딩 이력 조회",
            description = "해당 회원의 현재 멤버십에 대한 홀딩 이력 전체를 최신순으로 조회합니다.")
    ResponseEntity<ApiResponse<List<MembershipHoldResponse>>> getHolds(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @Parameter(description = "대상 회원 PK") @PathVariable Long memberPk,
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
