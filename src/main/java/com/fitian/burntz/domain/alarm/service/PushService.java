package com.fitian.burntz.domain.alarm.service;

import com.fitian.burntz.domain.alarm.entity.FcmToken;
import com.fitian.burntz.domain.alarm.repository.FcmTokenRepository;
import com.fitian.burntz.domain.alarm.v1.dto.PushDto;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.service
 * @fileName : PushService
 * @date : 2026-01-13
 * @description : 푸시알람 서비스 입니다.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PushService {

    private final FcmTokenRepository fcmTokenRepository;

    //한명의 유저가 사용하는 여러기기에 보내는 노티
    public void notifyUser(Long memberPk, PushDto dto) {
        List<String> tokens = getTokens(memberPk);

        if(tokens.isEmpty()){
            log.debug("No token for user {}", memberPk);
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(dto.getTitle())
                        .setBody(dto.getBody())
                        .build())
                .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.debug("Sent multicast to {} tokens : success = {}, failure = {}",
                    tokens.size(),response.getSuccessCount(),response.getFailureCount());

            for(int i = 0; i < response.getResponses().size(); i++) {
                SendResponse resp = response.getResponses().get(i);
                if(!resp.isSuccessful()) {
                    String token = tokens.get(i);
                    deleteToken(memberPk, token);
                    log.warn("Deleted FCM token {} due to error {}", token, resp.getException().getMessage());
                }
            }

        } catch (Exception ex) {
            log.error("Failed to send multicast FCM message", ex);
        }
    }

    public void notifyUserString(Long memberPk, String title, String body) {
        List<String> tokens = getTokens(memberPk);

        if(tokens.isEmpty()){
            log.debug("No token for user {}", memberPk);
            return;
        }

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            log.debug("Sent multicast to {} tokens : success = {}, failure = {}",
                    tokens.size(),response.getSuccessCount(),response.getFailureCount());

            for(int i = 0; i < response.getResponses().size(); i++) {
                SendResponse resp = response.getResponses().get(i);
                if(!resp.isSuccessful()) {
                    String token = tokens.get(i);
                    deleteToken(memberPk, token);
                    log.warn("Deleted FCM token {} due to error {}", token, resp.getException().getMessage());
                }
            }

        } catch (Exception ex) {
            log.error("Failed to send multicast FCM message", ex);
        }
    }

    //여러 유저의 각 사용기기에 보내는 노티
    public void notifyUsers(List<Long> memberPkList, PushDto dto) {

    }

    //memberPk로 토큰 리스트를 가져옵니다.
    private List<String> getTokens(Long memberPk) {
        List<FcmToken> tokenList = fcmTokenRepository.findTokenByMemberMemberPkAndDeletedYN(memberPk);
        return tokenList.stream()
                .map(FcmToken::getToken)
                .toList();
    }

    //토큰 삭제 메서드
    void deleteToken(Long memberPk, String token) {
        FcmToken fcmToken = fcmTokenRepository.findTokenByTokenAndDeletedYN(memberPk, token)
                .orElseThrow(() -> new ValidationException(ErrorCode.FCMTOKEN_NOT_FOUND));

        fcmToken.markDeleted();
        fcmTokenRepository.save(fcmToken);
    }
}
