package com.fitian.burntz.domain.record.v1.controller;

import com.fitian.burntz.domain.record.v1.dto.RecordCreateRequest;
import com.fitian.burntz.domain.record.v1.dto.RecordResponse;
import com.fitian.burntz.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

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
public class RecordController {

    /*
    * Record 생성
    * */
    @PostMapping()
    public ApiResponse<Void> createRecord(
            @Valid @RequestBody RecordCreateRequest request,
            @PathVariable Long boxPk,
            @PathVariable Long wodPk,
            @PathVariable Long ClassesPk
            ){
        return ApiResponse.success(null,"");
    }

    /*
     * Record 조회
     * */
    //classPk를 가져올지? -> 운동기록이 class당 1개니까 가져오긴해야할거같은데
    //api 추가해야하나?
    //아니면 body에 저장?
    @GetMapping
    public ApiResponse<RecordResponse> getRecord(
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            ){
        return ApiResponse.success("", "");
    }

}