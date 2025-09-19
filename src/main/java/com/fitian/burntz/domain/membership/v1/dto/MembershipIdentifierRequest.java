package com.fitian.burntz.domain.membership.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.membership.v1.dto
 * @fileName : MembershipDeleteRequest
 * @date : 2025-09-18
 * @description : 멤버십 삭제 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MembershipDeleteRequest", description = "멤버십 삭제 요청")
public class MembershipIdentifierRequest {
    @NotNull(message = "membershipPk must not be blank")
    @Schema(description = "멤버십 PK", example = "1")
    private Long membershipPk;
}
