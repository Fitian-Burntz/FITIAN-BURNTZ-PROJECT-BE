package com.fitian.burntz.domain.membership.docs;

import com.fitian.burntz.domain.membership.v2.dto.BoxHoldingPolicyRequest;
import com.fitian.burntz.domain.membership.v2.dto.BoxHoldingPolicyResponse;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "박스 홀딩 정책 API (v2)", description = "박스별 홀딩 최대 일수 및 횟수 정책을 설정합니다.")
public interface BoxHoldingPolicyDocs {

    @Operation(summary = "홀딩 정책 설정/수정 (운영자 전용)",
            description = """
                    박스의 홀딩 정책을 생성하거나 수정합니다 (upsert).
                    - maxHoldDaysPerMembership: 멤버십 1개당 총 홀딩 가능 일수. null이면 무제한.
                    - maxHoldCount: 멤버십 1개당 홀딩 가능 횟수. null이면 무제한.
                    - 회원별 커스텀 한도는 멤버십 수정 API에서 customMaxHoldDays로 설정합니다.
                    - MANAGER/OWNER만 호출 가능합니다.
                    """)
    ResponseEntity<ApiResponse<BoxHoldingPolicyResponse>> upsertPolicy(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @RequestBody BoxHoldingPolicyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "홀딩 정책 조회",
            description = "해당 박스의 홀딩 정책을 조회합니다. 정책이 없으면 404를 반환합니다.")
    ResponseEntity<ApiResponse<BoxHoldingPolicyResponse>> getPolicy(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
