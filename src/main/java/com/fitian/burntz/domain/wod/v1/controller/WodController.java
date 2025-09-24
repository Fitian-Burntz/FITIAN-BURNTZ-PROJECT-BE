package com.fitian.burntz.domain.wod.v1.controller;

import com.fitian.burntz.domain.wod.docs.WodDocs;
import com.fitian.burntz.domain.wod.service.WodService;
import com.fitian.burntz.domain.wod.v1.dto.WodCreateRequest;
import com.fitian.burntz.domain.wod.v1.dto.WodResponse;
import com.fitian.burntz.domain.wod.v1.dto.WodUpdateRequest;
import com.fitian.burntz.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.wod.v1.controller
 * @fileName : WodController
 * @date : 2025-09-16
 * @description : Wod Controller
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boxes/{boxPk}/wods")
public class WodController implements WodDocs {
    private final WodService wodService;

    /*
    * wod 생성
    * */
    @PostMapping()
    public ApiResponse<Void> createWod(
            @Valid @RequestBody WodCreateRequest request,
            @PathVariable Long boxPk
            //@AuthenticationPrincipal CustomUserDetails userDetails,
            ) {
        Long memberPk = 2L;
        wodService.createWod(request, boxPk, memberPk);
        return ApiResponse.success(null, "wod 생성 완료");
    }

    /*
    * wod 조회
    * */
    @GetMapping("/{date}")
    public ApiResponse<WodResponse> getWod(
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails,
    ){
        Long memberPk = 2L;
        WodResponse res = wodService.getWod(boxPk,memberPk, date);
        return ApiResponse.success(res,"해당 날짜의 Wod 조회 완료");
    }

    /*
    * wod 수정
    * */
    @PutMapping("/{date}")
    public ApiResponse<Void> updateWod(
            @Valid @RequestBody WodUpdateRequest request,
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long memberPk = 2L;
        wodService.updateWod(boxPk, memberPk, date,request);
        return ApiResponse.success(null,"해당 날짜의 Wod 수정 완료");
    }

    /*
    * wod 삭제
    * */
    @DeleteMapping("/{date}")
    public ApiResponse<Void> deleteWod(
            @PathVariable Long boxPk,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            //@AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Long memberPk = 2L;
        wodService.deleteWod(boxPk, memberPk, date);
        return ApiResponse.success(null,"해당 날짜의 Wod 삭제 완료");

    }

}