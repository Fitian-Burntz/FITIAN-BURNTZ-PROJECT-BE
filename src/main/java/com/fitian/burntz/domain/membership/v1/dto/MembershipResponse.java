package com.fitian.burntz.domain.membership.v1.dto;

import com.fitian.burntz.domain.membership.enums.MembershipStatus;
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
public class MembershipResponse {

    private Long membershipPk;
    private String membershipName;
    private LocalDate startDate;
    private LocalDate expirationDate;
    private MembershipStatus status;
    private String memo;
    private Long boxPk;
}
