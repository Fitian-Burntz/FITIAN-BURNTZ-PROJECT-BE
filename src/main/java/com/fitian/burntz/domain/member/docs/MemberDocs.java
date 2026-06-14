package com.fitian.burntz.domain.member.docs;

import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.dto.MemberInfoResponse;
import com.fitian.burntz.domain.member.dto.memberList_dto.BoxAndMemberListDto;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.fitian.burntz.infra.s3.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.domain.member.docs
 * @fileName : MemberDocs
 * @date : 2025-09-30
 * @description : MemberController 스웨거 설정
 */

@Tag(name = "member 관련 api 입니다.", description = "내 정보 조회, 업데이트, 탈퇴를 수행합니다.")
public interface MemberDocs {

    @Operation(summary = "내 정보 가져오기", description = "사용자의 내 정보를 가져옵니다.")
    public ResponseEntity<ApiResponse<MemberDto>> getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails);

    @Operation(summary = "내가 가입한 박스 정보들 가져오기", description = "사용자가 가입중인 박스들의 요약 정보를 가져옵니다.")
    public ResponseEntity<ApiResponse<List<BoxAndMemberListDto>>> getMyBoxes(@AuthenticationPrincipal CustomUserDetails customUserDetails);

    @Operation(summary = "내 정보 수정하기", description = "사용자의 내 정보(default nickname, gender)를 수정합니다.")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMemberInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String gender);

    @Operation(summary = "서비스 탈퇴하기", description = "사용자가 자발적으로 서비스를 탈퇴합니다. 탈퇴하더라도 재로그인 시 계정이 다시 활성화 됩니다.(휴먼계정 개념)")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> withdrawMember(
            @AuthenticationPrincipal CustomUserDetails customUserDetails);

    @Operation(
            summary = "프로필 이미지 업로드/변경",
            description = "지정한 박스의 프로필 이미지를 업로드합니다. 기존 이미지가 있으면 덮어씁니다. medium(800×800)·thumb(200×200) 두 사이즈로 저장되며, DB에는 thumb URL만 저장됩니다."
    )
    public ResponseEntity<ApiResponse<S3Service.ProfileImageUrls>> updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "박스 PK", required = true, example = "1") @RequestParam("boxPk") Long boxPk,
            @Parameter(description = "업로드할 이미지 파일 (최대 10MB, image/* 형식만 허용)", required = true) MultipartFile image);

    @Operation(
            summary = "프로필 이미지 삭제",
            description = "지정한 박스의 프로필 이미지를 S3에서 삭제하고 DB 값을 null로 초기화합니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteProfileImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "박스 PK", required = true, example = "1") @RequestParam("boxPk") Long boxPk);
}
