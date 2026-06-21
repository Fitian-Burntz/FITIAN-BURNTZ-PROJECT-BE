package com.fitian.burntz.domain.membership.v2.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MembershipHoldRequest {

    @NotNull
    private LocalDate holdStartDate;

    @NotNull
    private LocalDate holdEndDate;

    private String reason;
}
