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

    @Schema(description = "멤버십 PK", example = "1")
    private Long membershipPk;

    @Schema(description = "멤버십 로그 사유", example = "UPDATE", allowableValues = {"UPDATE","DELETE","CREATE"})
    private HistoryActionType actionType;

    @Schema(description = "이전 기록", example = "Json 형태")
    private String preValue;

    @Schema(description = "변경된 기록", example = "Json 형태")
    private String newValue;

    @Schema(description = "메모", example = "변경 사유")
    private String memo;

    @Schema(description = "기간", example = "1")
    private Integer period;

    @Schema(description = "변경한 사람", example = "Json member 객체")
    private Member createdBy;

    @Schema(description = "생성일시(서버 기준)", example = "2025-09-16T15:00:00")
    private LocalDateTime createdAt;
}
