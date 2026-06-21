package com.fitian.burntz.domain.locker.v2.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LockerCreateRequest {

    @NotNull
    @Min(1)
    private Integer startNumber;

    @NotNull
    @Min(1)
    private Integer endNumber;
}
