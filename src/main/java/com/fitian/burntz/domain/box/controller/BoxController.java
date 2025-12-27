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

    /** box 생성하기 **/
    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<BoxResponse>> createBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody CreateBoxRequest createBoxRequest
    ){
        preconditionValidator.requireLogin(customUserDetails);

        // 실제 생성 로직 실행 (예외는 글로벌 핸들러로 처리)
        BoxDto boxDtoResponse = boxService.createBox(createBoxRequest, customUserDetails);

        // 바디에는 간단한 메시지(원하면 null로 해도 됨)
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        BoxResponse.from(boxDtoResponse),
                        "Creation was successful."));

    }

    /** 활성화된 box boxPk로 단건 조회 **/
    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<BoxDto>> getBoxForPk(@RequestParam(value = "boxPk", required = false) Long boxPk){

        Long targetBoxPk = preconditionValidator.requireBoxPk(boxPk);

        BoxDto getBoxResponse = boxService.getBoxForPk(targetBoxPk);

        return ResponseEntity.ok(ApiResponse.success(getBoxResponse));
    }

    /** 활성화된 box boxCode로 단건 조회 **/
    @Override
    @GetMapping("/code")
    public ResponseEntity<ApiResponse<BoxDto>> getBoxForCode(@RequestParam(value = "boxCode", required = false) String boxCode){

        // 빈 문자열일 경우 null 처리
        String targetBoxCode = preconditionValidator.requireBoxCode(boxCode);

        BoxDto getBoxResponse = boxService.getBoxForBoxCode(targetBoxCode);

        return ResponseEntity.ok(ApiResponse.success(getBoxResponse));
    }


    /** 활성화된 box 리스트 전체 조회 **/
    @Override
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<BoxResponse>>> getAllActiveBoxes(
            @PageableDefault(size = 20, sort = "boxPk", direction = Sort.Direction.ASC) Pageable pageable) {

        // 클라이언트가 과도한 size 요청을 못하도록 방어
        Pageable safePageable = preconditionValidator.limitPageable(pageable, MAX_PAGE_SIZE);

        Page<BoxDto> boxDtoPage = boxService.getAllActiveBoxes(safePageable);

        // 컨트롤러 레이어에서 DTO -> Response 로 변환
        Page<BoxResponse> boxResponsePage = boxDtoPage.map(BoxResponse::from);

        return ResponseEntity.ok(ApiResponse.success(boxResponsePage));
    }

    /** box 에 회원 가입 **/
    @Override
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<JoinBoxDto>> joinMemberToBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxCode", required = false) String boxCode
    ){

        Long joinMemberPk = preconditionValidator.requireLogin(customUserDetails);

        String targetBoxCode = preconditionValidator.requireBoxCode(boxCode);

        JoinBoxDto joinResponse = boxService.joinMemberToBox(joinMemberPk, targetBoxCode);

        return ResponseEntity.ok(ApiResponse.success(joinResponse));

    }

    /** OWNER 가 박스 정보 수정 **/
    @Override
    @PutMapping
    public ResponseEntity<ApiResponse<UpdateBoxInfoDto>> updateInfoBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateBoxInfoRequest updateBoxInfoRequest
    ){

        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        UpdateBoxInfoDto updateBoxInfoResponse =
                boxService.updateBoxInfo(loginMemberPk, UpdateBoxInfoDto.from(updateBoxInfoRequest));

        return ResponseEntity.ok(ApiResponse.success(updateBoxInfoResponse));
    }


    /** box soft-delete
     * box 는 복구되지 않습니다.
     * 현재는 box soft-delete 만 되고 다른 연쇄 삭제처리는 하지 않습니다. **/
    @Override
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> deleteBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxPk", required = false) Long boxPk
    ){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        Long targetBoxPk = preconditionValidator.requireBoxPk(boxPk);

        boxService.removeBox(loginMemberPk, targetBoxPk);

        return ResponseEntity.ok(ApiResponse.success("Box has been deleted."));
    }


}
