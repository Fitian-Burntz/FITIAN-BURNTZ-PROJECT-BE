package com.fitian.burntz.domain.member.service;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.member.service
 * @fileName : BoxNicknameChangedEvent
 * @date : 2025-10-06
 * @description : 이벤트 데이터 전달용 클래스
 */
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class BoxNicknameChangedEvent {
    private final Long memberListPk;
    private final String oldNickname;
    private final String newNickname;
    private final List<Long> recordPks;
}