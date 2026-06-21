package com.fitian.burntz.domain.membership.v2.dto;

import com.fitian.burntz.domain.membership.entity.MembershipHold;
import com.fitian.burntz.domain.membership.enums.HoldStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class MembershipHoldResponse {

    private Long holdPk;
    private Long membershipPk;
    private LocalDate holdStartDate;
    private LocalDate holdEndDate;
    private LocalDate actualEndDate;
    private HoldStatus status;
    private String reason;
    private Long requestedByMemberPk;
    private Long cancelledByMemberPk;
    private LocalDate originalExpirationDate;

    public static MembershipHoldResponse from(MembershipHold hold) {
        return MembershipHoldResponse.builder()
                .holdPk(hold.getHoldPk())
                .membershipPk(hold.getMembership().getMembershipPk())
                .holdStartDate(hold.getHoldStartDate())
                .holdEndDate(hold.getHoldEndDate())
                .actualEndDate(hold.getActualEndDate())
                .status(hold.getStatus())
                .reason(hold.getReason())
                .requestedByMemberPk(hold.getRequestedBy().getMemberPk())
                .cancelledByMemberPk(hold.getCancelledBy() != null ? hold.getCancelledBy().getMemberPk() : null)
                .originalExpirationDate(hold.getOriginalExpirationDate())
                .build();
    }
}
