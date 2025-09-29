package com.fitian.burntz.domain.box.controller;

import com.fitian.burntz.domain.box.docs.BoxDocs;
import com.fitian.burntz.domain.box.dto.*;
import com.fitian.burntz.domain.box.service.BoxService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.common.util.PreconditionValidator;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boxes")
public class BoxController implements BoxDocs {

    private final BoxService boxService;
    private final PreconditionValidator preconditionValidator;
    private static final int MAX_PAGE_SIZE = 100;

  @GetMapping("/test")
  @Override
  public String test(@RequestParam String testValue) {
    return "Test API";
  }

    /** box 생성하기 **/
    @PostMapping
    public ResponseEntity<ApiResponse<BoxResponse>> createBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody CreateBoxRequest createBoxRequest
    ){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        // 실제 생성 로직 실행 (예외는 글로벌 핸들러로 처리)
        BoxDto boxDtoResponse = boxService.createBox(loginMemberPk, createBoxRequest);

        // 바디에는 간단한 메시지(원하면 null로 해도 됨)
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        BoxResponse.from(boxDtoResponse),
                        "Creation was successful."));

    }

    /** 활성화된 box boxPk로 단건 조회 **/
    @GetMapping
    public ResponseEntity<?> getBoxForPk(@RequestParam(value = "boxPk", required = false) Long boxPk){

        Long targetBoxPk = preconditionValidator.requireBoxPk(boxPk);

        BoxDto getBoxResponse = boxService.getBoxForPk(targetBoxPk);

        return ResponseEntity.ok(ApiResponse.success(getBoxResponse));
    }

    /** 활성화된 box boxCode로 단건 조회 **/
    @GetMapping("/code")
    public ResponseEntity<?> getBoxForCode(@RequestParam(value = "boxCode", required = false) String boxCode){

        // 빈 문자열일 경우 null 처리
        String targetBoxCode = preconditionValidator.requireBoxCode(boxCode);

        BoxDto getBoxResponse = boxService.getBoxForBoxCode(targetBoxCode);

        return ResponseEntity.ok(ApiResponse.success(getBoxResponse));
    }


    /** 활성화된 box 리스트 전체 조회 **/
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<BoxResponse>>> getAllActiveBoxes(
            @PageableDefault(size = 20, sort = "boxPk", direction = Sort.Direction.DESC) Pageable pageable) {

        // 클라이언트가 과도한 size 요청을 못하도록 방어
        Pageable safePageable = preconditionValidator.limitPageable(pageable, MAX_PAGE_SIZE);

        Page<BoxDto> boxDtoPage = boxService.getAllActiveBoxes(safePageable);

        // 컨트롤러 레이어에서 DTO -> Response 로 변환
        Page<BoxResponse> boxResponsePage = boxDtoPage.map(BoxResponse::from);

        return ResponseEntity.ok(ApiResponse.success(boxResponsePage));
    }

    /** box 에 회원 가입 **/
    @PostMapping("/join")
    public ResponseEntity<?> joinMemberToBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxCode", required = false) String boxCode
    ){

        Long joinMemberPk = preconditionValidator.requireLogin(customUserDetails);

        String targetBoxCode = preconditionValidator.requireBoxCode(boxCode);

        JoinBoxDto joinResponse = boxService.joinMemberToBox(joinMemberPk, targetBoxCode);

        return ResponseEntity.ok(ApiResponse.success(joinResponse));

    }

    @PutMapping
    public ResponseEntity<?> updateInfoBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateBoxInfoRequest updateBoxInfoRequest
    ){

        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        UpdateBoxInfoDto updateBoxInfoResponse = boxService.updateBoxInfo(loginMemberPk, UpdateBoxInfoDto.from(updateBoxInfoRequest));

        return ResponseEntity.ok(ApiResponse.success(updateBoxInfoResponse));
    }


    @DeleteMapping
    public ResponseEntity<?> deleteBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxPk", required = false) Long boxPk
    ){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        Long targetBoxPk = preconditionValidator.requireBoxPk(boxPk);

        boxService.removeBox(loginMemberPk, targetBoxPk);

        return ResponseEntity.ok(ApiResponse.success("Box has been deleted."));
    }


}
