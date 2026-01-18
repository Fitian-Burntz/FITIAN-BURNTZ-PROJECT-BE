package com.fitian.burntz.domain.alarm.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.v1.dto
 * @fileName : MessagePushResponse
 * @date : 2026-01-18
 * @description : 채팅 메시지 푸시 응답 dto 입니다.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessagePushResponse {
    private boolean dispatched;     // 이번 호출에서 실제 발송 로직을 수행했는지
    private boolean deduped;        // 멱등에 걸려서 스킵했는지
    private int targetCount;        // 대상 사용자 수
    private int tokenCount;         // 최종 토큰 수(distinct 후)
}
