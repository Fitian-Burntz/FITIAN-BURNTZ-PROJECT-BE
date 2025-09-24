package com.fitian.burntz.domain.wod.docs;

import com.fitian.burntz.domain.wod.v1.dto.WodCreateRequest;
import com.fitian.burntz.domain.wod.v1.dto.WodResponse;
import com.fitian.burntz.domain.wod.v1.dto.WodUpdateRequest;
import com.fitian.burntz.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.wod.docs
 * @fileName : WodDocs
 * @date : 2025-09-24
 * @description : Wod 관련 Swagger 문서
 */
@Tag(name = "Wod 관련 api 입니다.", description = "Wod를 생성하거나 수정, 삭제할 수 있습니다.")
public interface WodDocs {
    @Operation(summary = "Wod 생성", description = "해당 날짜의 Wod를 생성합니다.")
    ApiResponse<Void> createWod(
            @Valid @RequestBody WodCreateRequest request,
            @PathVariable Long boxPk
            //@AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Wod 조회", description = "지정한 박스(boxPk)와 날짜를 기준으로 해당 날짜의 WOD 정보를 조회합니다.")
    ApiResponse<WodResponse> getWod(
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails,
    );

    @Operation(summary = "Wod 수정", description = "지정한 박스(boxPk)와 날짜를 기준으로 기존 WOD 정보를 수정합니다.")
    ApiResponse<Void> updateWod(
            @Valid @RequestBody WodUpdateRequest request,
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Wod 삭제", description = "지정한 박스(boxPk)와 날짜를 기준으로 해당 날짜의 WOD를 삭제합니다.")
    ApiResponse<Void> deleteWod(
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    );
}
