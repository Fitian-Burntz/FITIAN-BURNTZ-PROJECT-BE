package com.fitian.burntz.domain.locker.v2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LockerCreateRequest {

    @NotBlank
    private String lockerNumber;
}
