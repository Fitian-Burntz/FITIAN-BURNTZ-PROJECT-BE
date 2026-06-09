package com.fitian.burntz.domain.channel.docs;

import com.fitian.burntz.domain.channel.entity.ChannelParticipant;
import com.fitian.burntz.domain.channel.v1.dto.*;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.docs
 * @fileName : ChannelDocs
 * @date : 2025-09-17
 * @description : 채널 관련 Swagger 문서입니다
 */

@Tag(name = "채널 관련 api 입니다.", description = "채널 생성하거나 수정, 삭제할 수 있습니다.")
public interface ChannelDocs {
    @Operation(summary = "채널 생성", description = "전체 또는 그룹 채팅을 생성합니다. OWNER/MANAGER 역할만 호출 가능합니다. 이미 동일한 channelId가 존재하면 기존 channelPk를 반환합니다.")
    ApiResponse<ChannelCreateResponse> createChannel(
            @Valid @RequestBody ChannelCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "채널 조회", description = "BoxPk와 현재 사용자 정보로 참여한 채널들을 조회합니다.")
    ResponseEntity<ApiResponse<List<ChannelListResponse>>> getChannels(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long boxPk
    );

    @Operation(summary = "채널 입장 유효성 확인", description = "ChannelPk와 현재 사용자 정보로 입장하려는 채널이 유효한지 확인합니다.")
    ResponseEntity<ApiResponse<List<ParticipantListResponse>>> getChannelEnter(
            @PathVariable Long channelPk,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "채널 참여자 조회", description = "채널 Pk로 채널 참여자를 반환합니다.")
    ResponseEntity<ApiResponse<List<ChannelParticipant>>> getParticipants(
            @PathVariable Long channelPk,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "채널 초대", description = "멤버Pk 리스트로 채널에 멤버를 초대합니다.")
    ApiResponse<Void> inviteParticipants(
            @Valid @RequestBody ChannelInviteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "참여자 삭제", description = "채널에서 참여자를 삭제합니다.")
    ApiResponse<Void> deleteParticipant(
            @Valid @RequestBody ChannelLeaveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "채널 삭제", description = "해당 채널을 삭제합니다.")
    ApiResponse<Void> deleteChanel(
            @Valid @RequestBody ChannelDeleteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
