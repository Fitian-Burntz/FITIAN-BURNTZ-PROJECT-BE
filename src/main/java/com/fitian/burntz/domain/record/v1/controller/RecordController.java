package com.fitian.burntz.domain.record.v1.controller;

import com.fitian.burntz.domain.record.docs.RecordDocs;
import com.fitian.burntz.domain.record.service.RecordService;
import com.fitian.burntz.domain.record.v1.dto.RecordCreateRequest;
import com.fitian.burntz.domain.record.v1.dto.RecordResponse;
import com.fitian.burntz.domain.record.v1.dto.RecordUpdateRequest;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.v1.controller
 * @fileName : RecordController
 * @date : 2025-09-17
 * @description : RecordController
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boxes/{boxPk}/wods/{date}/records")
public class RecordController implements RecordDocs {

    private final RecordService recordService;

    /*
    * Record 단건 생성
    * */
    @PostMapping("/single")
    public ApiResponse<Void> createRecord(
            @Valid @RequestBody RecordCreateRequest request,
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){
        recordService.createRecord(request, date, boxPk, userDetails.getMemberPk());
        return ApiResponse.success(null,"record 생성 완료");
    }

    /*
     * Record 다건 생성
     * */
    @PostMapping("/multi")
    public ApiResponse<Void> createRecords(
            @Valid @RequestBody List<@Valid RecordCreateRequest> request,
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        recordService.createRecords(request, date, boxPk, userDetails.getMemberPk());
        return ApiResponse.success(null,"records 생성 완료");
    }

    /*
     * Record 전체 조회(랭킹순)
     * */
    //classesPk, wodPk는 body에서 갖고오기
    @GetMapping()
    public ResponseEntity<ApiResponse<List<RecordResponse>>> getRecord(
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ){
        return ResponseEntity.ok((ApiResponse.success(recordService.getRecord(boxPk, userDetails.getMemberPk(), date),"해당 날짜의 records 조회 완료")));
    }

    /*
    * Record 수정
    * */
    @PutMapping("/{recordPk}")
    public ApiResponse<Void> updateRecord(
            @Valid @RequestBody RecordUpdateRequest request,
            @PathVariable Long boxPk,
            @PathVariable Long recordPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        recordService.updateRecord(boxPk,userDetails.getMemberPk(),recordPk,date,request);
        return ApiResponse.success(null,"record 수정 완료");
    }

    /*
     * 팀 레코드 삭제 예고 조회
     * */
    @GetMapping("/{recordPk}/team")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> getTeamRecordsToDelete(
            @PathVariable Long boxPk,
            @PathVariable Long recordPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                recordService.getTeamRecordsToDelete(boxPk, userDetails.getMemberPk(), recordPk)));
    }

    /*
    * Record 삭제
    * */
    @DeleteMapping("/{recordPk}")
    public ApiResponse<Void> deleteRecord(
            @PathVariable Long boxPk,
            @PathVariable Long recordPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        recordService.deleteRecord(boxPk, userDetails.getMemberPk(), recordPk, date);
        return ApiResponse.success(null,"record 삭제 완료");
    }

}