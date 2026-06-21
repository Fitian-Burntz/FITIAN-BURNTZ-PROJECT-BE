package com.fitian.burntz.domain.locker.v2.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class LockerAssignRequest {

    @NotNull
    private Long memberListPk;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
