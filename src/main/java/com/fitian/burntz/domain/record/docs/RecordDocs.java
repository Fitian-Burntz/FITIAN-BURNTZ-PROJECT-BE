package com.fitian.burntz.domain.record.docs;

import com.fitian.burntz.domain.record.v1.dto.RecordCreateRequest;
import com.fitian.burntz.domain.record.v1.dto.RecordResponse;
import com.fitian.burntz.domain.record.v1.dto.RecordUpdateRequest;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.List;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.docs
 * @fileName : RecordDocs
 * @date : 2025-09-24
 * @description : Record 관련 스웨거 문서
 */
@Tag(name = "Record 관련 api 입니다.", description = "Record를 생성하거나 수정, 삭제할 수 있습니다.")
public interface RecordDocs {

    @Operation(summary = "Record 생성", description = "지정한 박스(boxPk)와 날짜(WOD)에 대해 회원(MemberList 소속) 또는 비회원(일일체험) 참가자의 Record를 생성합니다. " +
                                                        "동일 클래스/참가자 중복 기록은 제한됩니다.")
    ApiResponse<Void> createRecord(
            @Valid @RequestBody RecordCreateRequest request,
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Record 조회", description = "지정한 박스(boxPk)와 날짜를 기준으로 해당 일자의 모든 Record 목록을 조회합니다. " +
                                                        "회원/비회원 기록을 모두 포합합니다.")
    ResponseEntity<ApiResponse<List<RecordResponse>>> getRecord(
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Record 수정", description = "지정한 박스(boxPk)와 날짜에 속한 특정 Record(recordPk)를 수정합니다.")
    ApiResponse<Void> updateRecord(
            @Valid @RequestBody RecordUpdateRequest request,
            @PathVariable Long boxPk,
            @PathVariable Long recordPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "Record 삭제", description = "지정한 박스(boxPk)와 날짜에 속한 특정 Record(recordPk)를 삭제합니다.")
    ApiResponse<Void> deleteRecord(
            @PathVariable Long boxPk,
            @PathVariable Long recordPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    );
}
