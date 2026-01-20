package com.fitian.burntz.domain.alarm.v1.controller;

import com.fitian.burntz.domain.alarm.docs.AlarmDocs;
import com.fitian.burntz.domain.alarm.entity.FcmToken;
import com.fitian.burntz.domain.alarm.service.AlarmService;
import com.fitian.burntz.domain.alarm.v1.dto.FcmTokenCreateRequest;
import com.fitian.burntz.domain.alarm.v1.dto.MessagePushRequest;
import com.fitian.burntz.domain.alarm.v1.dto.MessagePushResponse;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.controller
 * @fileName : AlarmController
 * @date : 2026-01-12
 * @description : FCM 알람을 담당하는 컨트롤러입니다.
 */

@RestController
@RequestMapping("/api/v1/alarm")
@RequiredArgsConstructor
public class AlarmController implements AlarmDocs {

    private final AlarmService alarmService;
    private final MemberListRepository memberListRepository;

    @PostMapping()
    public ApiResponse<Void> upsertToken(
            @Valid @RequestBody FcmTokenCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        FcmToken token = alarmService.upsertToken(userDetails, request);
        return ApiResponse.success(null, "TokenPk : "+token.getTokenPk()+" 토큰 등록.");
    }

    @GetMapping("/FirebaseCustomToken")
    public ResponseEntity<ApiResponse<String>> createFirebaseCustomToken(
            @AuthenticationPrincipal CustomUserDetails userDetails){
        try {
            List<Long> boxPks = memberListRepository.findBoxPksByMemberMemberPkAndDeletedYN(userDetails.getMemberPk(), BaseTime.Yn.N);

            Map<String, Object> claims = new HashMap<>();
            claims.put("boxPks",boxPks);
            claims.put("memberPk",userDetails.getMemberPk());
            String firebaseCustomToken = FirebaseAuth.getInstance().createCustomToken(userDetails.getMemberId(), claims);
            return ResponseEntity.ok(ApiResponse.success(firebaseCustomToken));
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Firebase 연동 실패: " + e.getMessage()));
        }
    }

    @PostMapping("/push-message")
    public ApiResponse<MessagePushResponse> pushMessage(
            @Valid @RequestBody MessagePushRequest request) {
        return ApiResponse.success(alarmService.dispatch(request));
    }
}
