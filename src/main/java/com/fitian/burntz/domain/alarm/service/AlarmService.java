package com.fitian.burntz.domain.alarm.service;

import com.fitian.burntz.domain.alarm.entity.FcmToken;
import com.fitian.burntz.domain.alarm.port.FirestorePushPort;
import com.fitian.burntz.domain.alarm.repository.FcmTokenRepository;
import com.fitian.burntz.domain.alarm.v1.dto.FcmTokenCreateRequest;
import com.fitian.burntz.domain.alarm.v1.dto.MessagePushRequest;
import com.fitian.burntz.domain.alarm.v1.dto.MessagePushResponse;
import com.fitian.burntz.domain.alarm.v1.dto.PushDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.service
 * @fileName : AlarmService
 * @date : 2026-01-12
 * @description : 알람 서비스 입니다.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlarmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final MemberRepository memberRepository;
    private final FirestorePushPort firestorePushPort;
    private final PushService pushService;

    public FcmToken upsertToken(CustomUserDetails userDetails, FcmTokenCreateRequest request) {
        return fcmTokenRepository.findTokenByMemberMemberPkAndDeviceIdAndDeletedYN(userDetails.getMemberPk(), request.getDeviceId())
                .orElseGet(() -> {
                    Member member = memberRepository.findById(userDetails.getMemberPk())
                            .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

                    List<FcmToken> fcmTokenList = fcmTokenRepository.findTokensByDeviceIdAndDeletedYN(request.getDeviceId(), BaseTime.Yn.N);
                    for(FcmToken t : fcmTokenList) {
                        t.markDeleted();
                    }

                    FcmToken newToken = FcmToken.builder()
                            .deviceId(request.getDeviceId())
                            .token(request.getToken())
                            .isActive("1")
                            .member(member)
                            .build();
                    return fcmTokenRepository.save(newToken);
                });
    }

    public MessagePushResponse dispatch(MessagePushRequest request) {

        boolean acquired = firestorePushPort.acquireDispatch(
                request.getBoxCode(),
                request.getChannelId(),
                request.getMessageId()
        );

        if(!acquired){
            return MessagePushResponse.builder()
                    .dispatched(false)
                    .deduped(true)
                    .targetCount(0)
                    .tokenCount(0)
                    .build();
        }

        try {
            LocalDateTime sentAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(request.getSentAtMillis()),
                    ZoneId.of("Asia/Seoul")
            );

            List<Long> memberPks = firestorePushPort.findMemberPksToNotify(
                    request.getBoxCode(),
                    request.getChannelId(),
                    sentAt
            );

            //보낸 사람 알람 제외
            if (request.getSenderId() != null) {
                memberPks = memberPks.stream()
                        .filter(pk -> !pk.equals(request.getSenderId()))
                        .toList();
            }

            if (memberPks.isEmpty()) {
                firestorePushPort.markSent(request.getBoxCode(), request.getChannelId(), request.getMessageId());
                return MessagePushResponse.builder()
                        .dispatched(true)
                        .deduped(false)
                        .targetCount(0)
                        .tokenCount(0)
                        .build();
            }

            PushDto dto = PushDto.builder()
                    .title("New Message!")
                    .body(request.getBoxNickname() + " : " + request.getText())
                    .build();

            pushService.notifyUsers(memberPks, dto);

            firestorePushPort.markSent(request.getBoxCode(), request.getChannelId(), request.getMessageId());

            return MessagePushResponse.builder()
                    .dispatched(true)
                    .deduped(false)
                    .targetCount(memberPks.size())
                    .tokenCount(0) // 여기서는 PushService가 토큰 수를 리턴하지 않으니 0 또는 제거 권장
                    .build();

        } catch (Exception e) {
            firestorePushPort.markFailed(
                    request.getBoxCode(),
                    request.getChannelId(),
                    request.getMessageId(),
                    safeReason(e)
            );
            throw e;
        }
    }

    private String safeReason(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        return msg.length() > 300 ? msg.substring(0, 300) : msg;
    }
}
