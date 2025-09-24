package com.fitian.burntz.domain.classes.v1.controller;

import com.fitian.burntz.domain.classes.docs.ClassesDocs;
import com.fitian.burntz.domain.classes.v1.dto.*;
import com.fitian.burntz.domain.classes.service.ClassesService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
* @author         : 김관중
* @packageName    : com.fitian.burntz.domain.classes.controller
* @fileName       : ClassesController
* @date           : 2025-09-15
* @description    : 수업 컨트롤러입니다.
**/
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassesController implements ClassesDocs {

    private final ClassesService classesService;

    @GetMapping()
    @Override
    public ResponseEntity<ApiResponse<List<ClassesResponse>>> getClasses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ClassesSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(classesService.getClasses(request, userDetails)));
    }

    @PostMapping()
    @Override
    public ApiResponse<Void> createClasses(
            @Valid @RequestBody List<ClassesCreateRequest> requestList,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        classesService.createClasses(requestList, userDetails);
        return ApiResponse.success(null,"수업 개설 완료.");
    }

    @PostMapping("/joinClass")
    @Override
    public ApiResponse<Void> joinClass(
            @Valid @RequestBody ClassesIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        classesService.joinClass(request, userDetails);
        return ApiResponse.success(null,"수업 참여 완료.");
    }

    @PostMapping("/cancelClass")
    @Override
    public ApiResponse<Void> cancelClass(
            @Valid @RequestBody ClassesIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        classesService.cancelClass(request, userDetails);
        return ApiResponse.success(null,"수업 취소 완료.");
    }

    @PostMapping("/getClassParticipant")
    @Override
    public ResponseEntity<ApiResponse<List<ClassParticipantResponse>>> getMembersByClassNo(
            @Valid @RequestBody ClassesIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(classesService.getMembersByClassPk(request, userDetails)));
    }

    @PostMapping("/update")
    @Override
    public ApiResponse<Void> updateClass(
            @Valid @RequestBody ClassesUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        classesService.updateClass(request, userDetails);
        return ApiResponse.success(null, "수업 변경 완료.");
    }

    @PostMapping("/delete")
    @Override
    public ApiResponse<Void> deleteClass(
            @Valid @RequestBody ClassesIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        classesService.deleteClass(request, userDetails);
        return ApiResponse.success(null, "수업 삭제 완료.");
    }
}
