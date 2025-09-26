package com.fitian.burntz.domain.membership.v1.dto;

import com.fitian.burntz.domain.membership.entity.Membership;

import java.time.LocalDate;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.membership.v1.dto
 * @fileName : MembershipHistorySnapshot
 * @date : 2025-09-26
 * @description : 멤버십 히스토리 직렬화 DTO 입니다
 */
public record MembershipHistorySnapshot(
        Long membershipPk,
        Long boxPk,
        Long memberPk,
        String membershipName,
        LocalDate startDate,
        LocalDate expirationDate,
        String status,
        String memo
) {
    public static MembershipHistorySnapshot from(Membership m) {
        if (m == null) return null;

        Long boxPk = null;
        try {
            if (m.getBox() != null) boxPk = m.getBox().getBoxPk();
        } catch (Exception ignored) {
            // 프록시가 완전히 초기화되지 않았거나 접근 문제시 null 로 둡니다.
        }

        Long memberPk = null;
        try {
            if (m.getMember() != null) memberPk = m.getMember().getMemberPk();
        } catch (Exception ignored) {
        }

        return new MembershipHistorySnapshot(
                m.getMembershipPk(),
                boxPk,
                memberPk,
                m.getMembershipName(),
                m.getStartDate(),
                m.getExpirationDate(),
                m.getStatus() != null ? m.getStatus().name() : null, // enum -> 문자열
                m.getMemo()
        );
    }
}
