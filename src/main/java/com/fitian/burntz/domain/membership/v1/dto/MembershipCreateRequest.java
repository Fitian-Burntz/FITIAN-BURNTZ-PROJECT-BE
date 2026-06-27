package com.fitian.burntz.domain.membership.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fitian.burntz.domain.membership.enums.MembershipStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.membership.v1.dto
 * @fileName : MembershipCreateRequest
 * @date : 2025-09-18
 * @description : 멤버십 생성 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MembershipCreateRequest", description = "멤버십 생성 요청")
public class MembershipCreateRequest {

    @Schema(description = "멤버십 이름", example = "새벽 회원권")
    private String membershipName;

    @NotNull(message = "startDate must not be blank")
    @Schema(description = "시작 일자", example = "2025-09-16")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "expirationDate must not be blank")
    @Schema(description = "종료 일자", example = "2025-09-16")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    @NotNull(message = "status is required")
    @Schema(description = "멤버십 상태", example = "ACTIVE", allowableValues = {"ACTIVE","DELETE","PENDING","EXPIRED"})
    private MembershipStatus status;

    @Schema(description = "메모", example = "지인 추천")
    private String memo;

    @NotNull(message = "period must not be blank")
    @Min(1)
    @Schema(description = "기간", example = "30")
    private Integer period;

    @Schema(description = "허용된 홀딩 가능 일수. null이면 홀딩 불가", example = "30")
    private Integer holdDays;
}
