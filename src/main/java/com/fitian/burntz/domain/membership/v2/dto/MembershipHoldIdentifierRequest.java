package com.fitian.burntz.domain.membership.v2.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MembershipHoldIdentifierRequest {

    @NotNull
    private Long holdPk;
}
