package com.fitian.burntz.domain.membership.v1.dto;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.membership.enums.HistoryActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.membership.v1.dto
 * @fileName : MembershipHistoryResponse
 * @date : 2025-09-18
 * @description : 멤버십 로그 응답 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MembershipHistoryResponse", description = "멤버십 로그 응답")
public class MembershipHistoryResponse {
    private Long membershipPk;
    private HistoryActionType actionType;
    private String preValue;
    private String newValue;
    private String memo;
    private Integer period;
    private Member createdBy;
    private LocalDateTime createdAt;
}
