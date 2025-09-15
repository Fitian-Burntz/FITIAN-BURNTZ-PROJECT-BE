package com.fitian.burntz.domain.classes.v1.controller;

import com.fitian.burntz.domain.classes.v1.dto.ClassesCreateRequest;
import com.fitian.burntz.domain.classes.v1.dto.ClassesJoinRequest;
import com.fitian.burntz.domain.classes.v1.dto.ClassesSearchRequest;
import com.fitian.burntz.domain.classes.entity.Classes;
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
public class ClassesController {

    private final ClassesService classesService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<Classes>>> getClasses(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ClassesSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(classesService.getClasses(request, userDetails)));
    }

    @PostMapping()
    public ApiResponse<Void> createClasses(
            @Valid @RequestBody List<ClassesCreateRequest> requestList,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        classesService.createClasses(requestList, userDetails);
        return ApiResponse.success(null,"수업 개설 완료.");
    }

    @PostMapping("/joinClass")
    public ApiResponse<Void> joinClass(
            @Valid @RequestBody ClassesJoinRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        classesService.joinClass(request, userDetails);
        return ApiResponse.success(null,"수업 참여 완료.");
    }
}
