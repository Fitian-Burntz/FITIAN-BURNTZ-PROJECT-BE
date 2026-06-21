package com.fitian.burntz.domain.locker.v2.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class LockerResponse {

    private Long lockerPk;
    private String lockerNumber;
    private String status; // AVAILABLE, OCCUPIED
    private Long lockerUsagePk;
    private Long memberListPk;
    private String assignedTo;
    private LocalDate startDate;
    private LocalDate endDate;
}
