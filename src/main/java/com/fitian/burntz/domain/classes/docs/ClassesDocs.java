package com.fitian.burntz.domain.classes.docs;

import com.fitian.burntz.domain.classes.v1.dto.*;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.docs
 * @fileName : ClassesDocs
 * @date : 2025-09-16
 * @description : 수업 관련 Swagger 문서입니다
 */

@Tag(name = "수업 관련 api 입니다.", description = "수업 생성하거나 수정, 삭제할 수 있습니다.")
public interface ClassesDocs {
    @Operation(summary = "수업 목록 조회", description = "기간 조건으로 박스의 수업을 조회합니다.")
    ResponseEntity<ApiResponse<List<ClassesResponse>>> getClasses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ClassesSearchRequest request
    );

    @Operation(summary = "수업 목록 조회", description = "날짜로 기록유무를 포함한 박스의 수업 및 참여자를 조회합니다.")
    ResponseEntity<ApiResponse<List<ClassesWithParticipant>>> getClassesWithRecords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute ClassesWithRecordsSearchRequest request
    );

    @Operation(summary = "수업 다건 생성", description = "요청 리스트를 받아 여러 수업을 한 번에 생성합니다.")
    ApiResponse<Void> createClasses(
            @Valid @RequestBody List<ClassesCreateRequest> requestList,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "수업 참여", description = "특정 수업에 현재 사용자로 참여합니다.")
    ApiResponse<Void> joinClass(
            @Valid @RequestBody ClassesIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "수업 참여 취소", description = "특정 수업 참여를 취소합니다.")
    ApiResponse<Void> cancelClass(
            @Valid @RequestBody ClassesIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "수업 참여자 목록", description = "특정 수업의 참여자 목록을 반환합니다.")
    ResponseEntity<ApiResponse<List<ClassParticipantResponse>>> getMembersByClassNo(
            @Valid @RequestBody ClassesIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "수업 변경", description = "수업의 시간/정원/제목/메모 등을 변경합니다.")
    ApiResponse<Void> updateClass(
            @Valid @RequestBody ClassesUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "수업 삭제", description = "수업을 삭제(soft-delete)합니다.")
    ApiResponse<Void> deleteClass(
            @Valid @RequestBody ClassesIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "수업 일자별 삭제", description = "특정일자의 수업을 잔체 삭제(soft-delete)합니다.")
    ApiResponse<Void> deleteClassByDate(
            @Valid @RequestBody ClassesDeleteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
