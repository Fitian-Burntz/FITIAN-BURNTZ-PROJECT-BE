package com.fitian.burntz.domain.membership.v1.dto;

import com.fitian.burntz.domain.membership.enums.MembershipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.membership.dto
 * @fileName : MembershipResponse
 * @date : 2025-09-17
 * @description : 멤버십 응답 DTO 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MembershipResponse", description = "멤버십 반환 모델")
public class MembershipResponse {

    @Schema(description = "멤버십 PK", example = "1")
    private Long membershipPk;

    @Schema(description = "멤버십 이름", example = "새벽 회원권")
    private String membershipName;

    @Schema(description = "시작 일자", example = "2025-09-16")
    private LocalDate startDate;

    @Schema(description = "종료 일자", example = "2025-09-16")
    private LocalDate expirationDate;

    @Schema(description = "멤버십 상태", example = "ACTIVE", allowableValues = {"ACTIVE","DELETE","PENDING","EXPIRED"})
    private MembershipStatus status;

    @Schema(description = "메모", example = "지인 추천")
    private String memo;

    @Schema(description = "박스 PK", example = "1")
    private Long boxPk;

    @Schema(description = "박스 닉네임", example = "문정동 이경영")
    private String boxNickname;

    @Schema(description = "허용된 홀딩 가능 일수. null이면 홀딩 불가", example = "30")
    private Integer holdDays;

    @Schema(description = "지금까지 사용한 홀딩 일수 (ACTIVE+COMPLETED 합산)", example = "10")
    private Integer usedHoldDays;

}
