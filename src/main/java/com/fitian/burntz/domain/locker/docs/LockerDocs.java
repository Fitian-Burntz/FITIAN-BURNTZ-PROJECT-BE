package com.fitian.burntz.domain.locker.docs;

import com.fitian.burntz.domain.locker.v2.dto.*;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "사물함 API (v2)", description = "사물함 등록, 조회, 배정, 해제를 수행합니다.")
public interface LockerDocs {

    @Operation(summary = "사물함 일괄 등록 (운영자)",
            description = "startNumber ~ endNumber 범위의 사물함을 한 번에 등록합니다. 이미 존재하는 번호가 포함되면 전체 실패합니다.")
    ApiResponse<Void> createLocker(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @Valid @RequestBody LockerCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "사물함 목록 조회 (운영자)",
            description = "박스의 사물함 전체 목록과 배정 현황을 조회합니다. status: AVAILABLE(비어있음) / OCCUPIED(배정됨)")
    ApiResponse<List<LockerResponse>> getLockers(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "사물함 삭제 (운영자)",
            description = "사물함을 삭제합니다. 현재 배정된 이용권이 있으면 삭제할 수 없습니다.")
    ApiResponse<Void> deleteLocker(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @Parameter(description = "사물함 PK") @PathVariable Long lockerPk,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "사물함 배정 (운영자)",
            description = "특정 사물함을 회원에게 배정합니다. 이미 배정된 사물함에는 중복 배정할 수 없습니다.")
    ApiResponse<Void> assignLocker(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @Parameter(description = "사물함 PK") @PathVariable Long lockerPk,
            @Valid @RequestBody LockerAssignRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "사물함 이용권 해제 (운영자)",
            description = "배정된 사물함 이용권을 해제합니다. 해제 후 해당 사물함은 AVAILABLE 상태로 전환됩니다.")
    ApiResponse<Void> revokeLocker(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @Parameter(description = "사물함 PK") @PathVariable Long lockerPk,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "내 사물함 조회 (회원)",
            description = "로그인한 회원의 현재 박스에서 배정된 사물함을 조회합니다. 배정된 사물함이 없으면 404를 반환합니다.")
    ApiResponse<MyLockerResponse> getMyLocker(
            @Parameter(description = "박스 PK") @PathVariable Long boxPk,
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
