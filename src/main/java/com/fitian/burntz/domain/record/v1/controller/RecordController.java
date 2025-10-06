package com.fitian.burntz.domain.record.v1.controller;

import com.fitian.burntz.domain.channel.docs.ChannelDocs;
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
    * Record 생성
    * */
    @PostMapping()
    public ApiResponse<Void> createRecord(
            @Valid @RequestBody RecordCreateRequest request,
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
            ){
        Long memberPk = 2L;
        //recordService.createRecord(request, date, boxPk, userDetails.getMemberPk());
        recordService.createRecord(request, date, boxPk, memberPk);
        return ApiResponse.success(null,"record 생성 완료");
    }

    /*
     * Record 전체 조회(랭킹순)
     * */
    //classesPk, wodPk는 body에서 갖고오기
    @GetMapping()
    public ResponseEntity<ApiResponse<List<RecordResponse>>> getRecord(
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
            ){
        Long memberPk = 2L;
        return ResponseEntity.ok((ApiResponse.success(recordService.getRecord(boxPk, memberPk, date),"해당 날짜의 records 조회 완료")));
//        return ResponseEntity.ok((ApiResponse.success(recordService.getRecord(boxPk, userDetails.getMemberPk(), date),"해당 날짜의 records 조회 완료")));
    }

    /*
    * Record 수정
    * */
    @PutMapping("/{recordPk}")
    public ApiResponse<Void> updateRecord(
            @Valid @RequestBody RecordUpdateRequest request,
            @PathVariable Long boxPk,
            @PathVariable Long recordPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long memberPk = 2L;
        //recordService.updateRecord(boxPk,userDetails.getMemberPk(),recordPk,date,request);
        recordService.updateRecord(boxPk,memberPk,recordPk,date,request);
        return ApiResponse.success(null,"record 수정 완료");
    }

    /*
    * Record 삭제
    * */
    @DeleteMapping("/{recordPk}")
    public ApiResponse<Void> deleteRecord(
            @PathVariable Long boxPk,
            @PathVariable Long recordPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long memberPk = 2L;
        recordService.deleteRecord(boxPk, memberPk, recordPk, date);
        //recordService.deleteRecord(boxPk, userDetails.getMemberPk(), recordPk, date);
        return ApiResponse.success(null,"record 삭제 완료");
    }

}