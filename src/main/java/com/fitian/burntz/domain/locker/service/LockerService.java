package com.fitian.burntz.domain.locker.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.locker.entity.Locker;
import com.fitian.burntz.domain.locker.entity.LockerUsage;
import com.fitian.burntz.domain.locker.enums.LockerUsageStatus;
import com.fitian.burntz.domain.locker.repository.LockerRepository;
import com.fitian.burntz.domain.locker.repository.LockerUsageRepository;
import com.fitian.burntz.domain.locker.v2.dto.*;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.global.common.entity.BaseTime.Yn;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LockerService {

    private final LockerRepository lockerRepository;
    private final LockerUsageRepository lockerUsageRepository;
    private final MemberListRepository memberListRepository;
    private final BoxRepository boxRepository;

    public void createLocker(Long boxPk, LockerCreateRequest request, CustomUserDetails userDetails) {
        requireManagerOrOwner(userDetails.getMemberPk(), boxPk);

        if (lockerRepository.existsByBoxBoxPkAndLockerNumberAndDeletedYN(boxPk, request.getLockerNumber(), Yn.N)) {
            throw new ValidationException(ErrorCode.LOCKER_NUMBER_DUPLICATE);
        }

        Box box = boxRepository.findById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        lockerRepository.save(Locker.builder()
                .box(box)
                .lockerNumber(request.getLockerNumber())
                .build());
    }

    @Transactional(readOnly = true)
    public List<LockerResponse> getLockers(Long boxPk, CustomUserDetails userDetails) {
        requireManagerOrOwner(userDetails.getMemberPk(), boxPk);

        List<Locker> lockers = lockerRepository.findAllByBoxBoxPkAndDeletedYN(boxPk, Yn.N);

        Map<Long, LockerUsage> activeUsageByLockerPk = lockerUsageRepository
                .findActiveUsagesByBoxPk(boxPk, LockerUsageStatus.ACTIVE, Yn.N)
                .stream()
                .collect(Collectors.toMap(lu -> lu.getLocker().getLockerPk(), lu -> lu));

        return lockers.stream().map(locker -> {
            LockerUsage usage = activeUsageByLockerPk.get(locker.getLockerPk());
            if (usage != null) {
                return LockerResponse.builder()
                        .lockerPk(locker.getLockerPk())
                        .lockerNumber(locker.getLockerNumber())
                        .status("OCCUPIED")
                        .lockerUsagePk(usage.getLockerUsagePk())
                        .memberListPk(usage.getMemberList().getMemberListPk())
                        .assignedTo(usage.getMemberList().getBoxNickname())
                        .startDate(usage.getStartDate())
                        .endDate(usage.getEndDate())
                        .build();
            }
            return LockerResponse.builder()
                    .lockerPk(locker.getLockerPk())
                    .lockerNumber(locker.getLockerNumber())
                    .status("AVAILABLE")
                    .build();
        }).collect(Collectors.toList());
    }

    public void deleteLocker(Long boxPk, Long lockerPk, CustomUserDetails userDetails) {
        requireManagerOrOwner(userDetails.getMemberPk(), boxPk);

        Locker locker = lockerRepository.findByLockerPkAndBoxBoxPkAndDeletedYN(lockerPk, boxPk, Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.LOCKER_NOT_FOUND));

        lockerUsageRepository.findByLockerLockerPkAndStatusAndDeletedYN(lockerPk, LockerUsageStatus.ACTIVE, Yn.N)
                .ifPresent(u -> { throw new ValidationException(ErrorCode.LOCKER_ALREADY_ASSIGNED); });

        locker.markDeleted();
    }

    public void assignLocker(Long boxPk, Long lockerPk, LockerAssignRequest request, CustomUserDetails userDetails) {
        requireManagerOrOwner(userDetails.getMemberPk(), boxPk);

        Locker locker = lockerRepository.findByLockerPkAndBoxBoxPkAndDeletedYN(lockerPk, boxPk, Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.LOCKER_NOT_FOUND));

        if (lockerUsageRepository.findByLockerLockerPkAndStatusAndDeletedYN(lockerPk, LockerUsageStatus.ACTIVE, Yn.N).isPresent()) {
            throw new ValidationException(ErrorCode.LOCKER_ALREADY_ASSIGNED);
        }

        MemberList memberList = memberListRepository.findById(request.getMemberListPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        lockerUsageRepository.save(LockerUsage.builder()
                .locker(locker)
                .memberList(memberList)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(LockerUsageStatus.ACTIVE)
                .build());
    }

    public void revokeLocker(Long boxPk, Long lockerPk, CustomUserDetails userDetails) {
        requireManagerOrOwner(userDetails.getMemberPk(), boxPk);

        lockerRepository.findByLockerPkAndBoxBoxPkAndDeletedYN(lockerPk, boxPk, Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.LOCKER_NOT_FOUND));

        LockerUsage usage = lockerUsageRepository
                .findByLockerLockerPkAndStatusAndDeletedYN(lockerPk, LockerUsageStatus.ACTIVE, Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.LOCKER_USAGE_NOT_FOUND));

        usage.markDeleted();
    }

    @Transactional(readOnly = true)
    public MyLockerResponse getMyLocker(Long boxPk, CustomUserDetails userDetails) {
        MemberList memberList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(
                        userDetails.getMemberPk(), boxPk, Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        LockerUsage usage = lockerUsageRepository
                .findByMemberListMemberListPkAndLockerBoxBoxPkAndStatusAndDeletedYN(
                        memberList.getMemberListPk(), boxPk, LockerUsageStatus.ACTIVE, Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.LOCKER_USAGE_NOT_FOUND));

        return MyLockerResponse.builder()
                .lockerPk(usage.getLocker().getLockerPk())
                .lockerNumber(usage.getLocker().getLockerNumber())
                .startDate(usage.getStartDate())
                .endDate(usage.getEndDate())
                .status(usage.getStatus())
                .build();
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void checkLockerExpiration() {
        List<LockerUsage> expired = lockerUsageRepository
                .findAllByEndDateBeforeAndStatusAndDeletedYN(LocalDate.now(), LockerUsageStatus.ACTIVE, Yn.N);

        expired.forEach(LockerUsage::expire);
        log.info("[LOCKER] 만료 처리 완료: {}건", expired.size());
    }

    private void requireManagerOrOwner(Long memberPk, Long boxPk) {
        MemberList ml = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(memberPk, boxPk, Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if (ml.getRole() == MemberRole.GUEST || ml.getRole() == MemberRole.MEMBER) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }
    }
}
