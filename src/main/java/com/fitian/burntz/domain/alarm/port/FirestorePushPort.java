package com.fitian.burntz.domain.alarm.port;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.port
 * @fileName : FirestorePushPort
 * @date : 2026-01-18
 * @description :
 */
public interface FirestorePushPort {

    /**
     * messageId 단위로 "이번 요청이 최초 처리자" 인지 선점한다.
     * @return true면 최초 처리자(계속 진행), false면 이미 처리됨(즉시 종료)
     */

    boolean acquireDispatch(String boxCode, String channelId, String messageId);

    void markSent(String boxCode, String channelId, String messageId);

    void markFailed(String boxCode, String channelId, String messageId, String reason);

    /**
     * participants 중 lastReadAt < sentAt 인 사용자(memberPk) 리스트를 반환
     */
    List<Long> findMemberPksToNotify(String boxCode, String channelId, LocalDateTime sentAt);
}
