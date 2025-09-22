package com.fitian.burntz.domain.box.controller;

import com.fitian.burntz.domain.box.docs.BoxDocs;
import com.fitian.burntz.domain.box.dto.BoxDto;
import com.fitian.burntz.domain.box.dto.BoxResponse;
import com.fitian.burntz.domain.box.dto.CreateBoxRequest;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.service.BoxService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boxes")
public class BoxController implements BoxDocs {

    private final BoxService boxService;
    private static final int MAX_PAGE_SIZE = 100;

  @GetMapping("/test")
  @Override
  public String test(@RequestParam String testValue) {
    return "Test API";
  }

    /** 활성화된 box 리스트 전체 조회 **/
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<BoxResponse>>> getAllActiveBoxes(
            @PageableDefault(size = 20, sort = "boxPk", direction = Sort.Direction.DESC) Pageable pageable) {

        // 클라이언트가 과도한 size 요청을 못하도록 방어
        int safeSize = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Pageable safePageable = PageRequest.of(pageable.getPageNumber(), safeSize, pageable.getSort());

        Page<BoxDto> boxDtoPage = boxService.getAllActiveBoxes(safePageable);

        // 컨트롤러 레이어에서 DTO -> Response 로 변환
        Page<BoxResponse> boxResponsePage = boxDtoPage.map(BoxResponse::from);

        return ResponseEntity.ok(ApiResponse.success(boxResponsePage));
    }


    @PostMapping
    public ResponseEntity<ApiResponse<BoxResponse>> createBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody CreateBoxRequest createBoxRequest
    ){
        Long memberPk = customUserDetails.getMemberPk();

        // 실제 생성 로직 실행 (예외는 글로벌 핸들러로 처리)
        BoxDto boxDtoResponse = boxService.createBox(memberPk, createBoxRequest);

        // 바디에는 간단한 메시지(원하면 null로 해도 됨)
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        BoxResponse.from(boxDtoResponse),
                        "Creation was successful."));

    }


}
