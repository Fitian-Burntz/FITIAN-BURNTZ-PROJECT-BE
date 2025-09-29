package com.fitian.burntz.domain.membership.v1.dto;

import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.enums.MembershipStatus;
import lombok.*;

import java.time.LocalDate;
import java.util.Objects;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.domain.membership.v1.dto
 * @fileName : MembershipDto
 * @date : 2025-09-25
 * @description : 멤버 리스트 반환용 membershipDto 입니다.
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MembershipDto {

    private Long membershipPk;
    private String membershipName;
    private LocalDate startDate;
    private LocalDate expirationDate;
    private MembershipStatus status;
    private String memo;

    public static MembershipDto from(Membership membership) {
        Objects.requireNonNull(membership);

        return MembershipDto.builder()
                .membershipPk(membership.getMembershipPk())
                .membershipName(membership.getMembershipName())
                .startDate(membership.getStartDate())
                .expirationDate(membership.getExpirationDate())
                .status(membership.getStatus())
                .memo(membership.getMemo())
                .build();
    }
}
