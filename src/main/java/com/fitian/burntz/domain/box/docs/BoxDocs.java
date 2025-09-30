package com.fitian.burntz.domain.box.docs;

import com.fitian.burntz.domain.box.dto.BoxResponse;
import com.fitian.burntz.domain.box.dto.CreateBoxRequest;
import com.fitian.burntz.domain.box.dto.UpdateBoxInfoRequest;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "박스 관련 api 입니다.", description = "box 생성, 조회, 가입, 삭제를 수행합니다.")
public interface BoxDocs {

    @Operation(summary = "box 생성", description = "box를 생성하고 box를 생성한 사용자는 자동으로 해당 box 의 OWNER 가 됩니다.")
    public ResponseEntity<com.fitian.burntz.global.common.response.ApiResponse<BoxResponse>> createBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody CreateBoxRequest createBoxRequest
    );

    @Operation(summary = "boxPk 로 box 단건 조회", description = "boxPk 로 box 를 단건 조회합니다.")
    public ResponseEntity<?> getBoxForPk(@RequestParam(value = "boxPk", required = false) Long boxPk);

    @Operation(summary = "boxCode 로 box 단건 조회", description = "boxCode 로 box 를 단건 조회합니다.")
    public ResponseEntity<?> getBoxForCode(@RequestParam(value = "boxCode", required = false) String boxCode);

    @Operation(summary = "box 전체 조회 페이징", description = "사용자가 box 에 가입 시 box를 찾을 수 있도록 오름차순으로 리스트를 페이징 해서 줍니다.")
    public ResponseEntity<ApiResponse<Page<BoxResponse>>> getAllActiveBoxes(
            @PageableDefault(size = 20, sort = "boxPk", direction = Sort.Direction.DESC) Pageable pageable);

    @Operation(summary = "사용자가 box 에 가입", description = "사용자가 boxCode 를 사용해 box 에 최초 가입합니다. 최초 가입 시 권한은 GUEST 입니다.")
    public ResponseEntity<?> joinMemberToBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxCode", required = false) String boxCode
    );


    @Operation(summary = "box 정보 수정", description = "owner 가 box 정보를 수정할 수 있습니다.")
    public ResponseEntity<?> updateInfoBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateBoxInfoRequest updateBoxInfoRequest
    );

    @Operation(summary = "box 삭제", description = "box 를 소프트삭제합니다. box 는 삭제되면 다시 복구되지 않습니다.")
    public ResponseEntity<?> deleteBox(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxPk", required = false) Long boxPk
    );
}
