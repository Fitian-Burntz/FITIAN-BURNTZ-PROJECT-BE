package com.fitian.burntz.domain.locker.v2.dto;

import com.fitian.burntz.domain.locker.enums.LockerUsageStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class MyLockerResponse {

    private Long lockerPk;
    private String lockerNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private LockerUsageStatus status;
}
