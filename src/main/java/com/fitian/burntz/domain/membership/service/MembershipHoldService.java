package com.fitian.burntz.domain.membership.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitian.burntz.domain.alarm.service.PushService;
import com.fitian.burntz.domain.alarm.v1.dto.PushDto;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.classes.repository.ClassParticipantRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.entity.MembershipHistory;
import com.fitian.burntz.domain.membership.entity.MembershipHold;
import com.fitian.burntz.domain.membership.enums.HistoryActionType;
import com.fitian.burntz.domain.membership.enums.HoldStatus;
import com.fitian.burntz.domain.membership.enums.MembershipStatus;
import com.fitian.burntz.domain.membership.repository.MembershipHistoryRepository;
import com.fitian.burntz.domain.membership.repository.MembershipHoldRepository;
import com.fitian.burntz.domain.membership.repository.MembershipRepository;
import com.fitian.burntz.domain.membership.v1.dto.MembershipHistorySnapshot;
import com.fitian.burntz.domain.membership.v2.dto.MembershipHoldIdentifierRequest;
import com.fitian.burntz.domain.membership.v2.dto.MembershipHoldRequest;
import com.fitian.burntz.domain.membership.v2.dto.MembershipHoldResponse;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MembershipHoldService {

    private static final List<HoldStatus> USED_HOLD_STATUSES = List.of(HoldStatus.ACTIVE, HoldStatus.COMPLETED);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final MemberListRepository memberListRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipHoldRepository holdRepository;
    private final MembershipHistoryRepository historyRepository;
    private final ClassParticipantRepository classParticipantRepository;
    private final PushService pushService;

    public MembershipHoldResponse createHold(Long boxPk, Long memberPk, MembershipHoldRequest request, CustomUserDetails userDetails) {
        LocalDate today = LocalDate.now(KST);

        if (!request.getHoldStartDate().isBefore(request.getHoldEndDate())) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (request.getHoldStartDate().isBefore(today)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
        }

        MemberList requesterList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        // 회원 본인이거나 운영자(MANAGER/OWNER)만 신청 가능
        boolean isManager = requesterList.getRole() == MemberRole.MANAGER || requesterList.getRole() == MemberRole.OWNER;
        if (!isManager && !userDetails.getMemberPk().equals(memberPk)) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED);
        }

        Membership membership = membershipRepository.findAllMembershipByBoxPkAndMemberPk(boxPk, memberPk)
                .stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE || m.getStatus() == MembershipStatus.HOLDING)
                .findFirst()
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        validateHoldPolicy(membership, request.getHoldStartDate(), request.getHoldEndDate());

        // 날짜 겹침 검증
        List<MembershipHold> overlapping = holdRepository.findOverlapping(
                membership.getMembershipPk(), request.getHoldStartDate(), request.getHoldEndDate());
        if (!overlapping.isEmpty()) {
            throw new ValidationException(ErrorCode.HOLD_OVERLAPPING);
        }

        Member requester = memberRepository.findById(userDetails.getMemberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        boolean startsToday = !request.getHoldStartDate().isAfter(today);
        HoldStatus initialStatus = startsToday ? HoldStatus.ACTIVE : HoldStatus.SCHEDULED;

        MembershipHold hold = MembershipHold.builder()
                .membership(membership)
                .holdStartDate(request.getHoldStartDate())
                .holdEndDate(request.getHoldEndDate())
                .status(initialStatus)
                .requestedBy(requester)
                .reason(request.getReason())
                .originalExpirationDate(membership.getExpirationDate())
                .build();

        holdRepository.save(hold);

        if (startsToday) {
            membership.startHolding();
            cancelClassesInHoldPeriod(memberPk, boxPk, request.getHoldStartDate(), request.getHoldEndDate());
        }

        saveHistory(membership, requester, HistoryActionType.HOLD,
                "홀딩 신청: " + request.getHoldStartDate() + " ~ " + request.getHoldEndDate()
                + (request.getReason() != null ? " / 사유: " + request.getReason() : ""));

        // 운영자들에게 알림
        notifyManagers(boxPk, memberPk, membership);

        return MembershipHoldResponse.from(hold);
    }

    // 조기 종료: 회원/운영자 모두 가능. ACTIVE → COMPLETED (만료일 재계산), SCHEDULED → CANCELLED
    public void endHold(Long boxPk, Long memberPk, MembershipHoldIdentifierRequest request, CustomUserDetails userDetails) {
        LocalDate today = LocalDate.now(KST);

        MemberList requesterList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        boolean isManager = requesterList.getRole() == MemberRole.MANAGER || requesterList.getRole() == MemberRole.OWNER;
        if (!isManager && !userDetails.getMemberPk().equals(memberPk)) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED);
        }

        MembershipHold hold = holdRepository.findById(request.getHoldPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.HOLD_NOT_FOUND));

        if (hold.getStatus() == HoldStatus.COMPLETED || hold.getStatus() == HoldStatus.CANCELLED) {
            throw new ValidationException(ErrorCode.HOLD_NOT_CANCELLABLE);
        }

        Membership membership = hold.getMembership();
        Member requester = memberRepository.findById(userDetails.getMemberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        if (hold.getStatus() == HoldStatus.ACTIVE) {
            hold.complete(today);
            // B안: newExpirationDate = actualEndDate + (originalExpirationDate - holdStartDate)
            long remainingDays = ChronoUnit.DAYS.between(hold.getHoldStartDate(), hold.getOriginalExpirationDate());
            membership.resumeActive(today.plusDays(remainingDays));
        } else {
            // SCHEDULED → 그냥 취소 (멤버십 상태 변경 없음)
            hold.cancel(requester);
        }

        saveHistory(membership, requester, HistoryActionType.UNHOLD, "홀딩 조기 종료: holdPk=" + hold.getHoldPk());
    }

    // 롤백: 운영자 전용. 만료일을 originalExpirationDate로 완전 복원
    public void cancelHold(Long boxPk, Long memberPk, MembershipHoldIdentifierRequest request, CustomUserDetails userDetails) {
        MemberList requesterList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        if (requesterList.getRole() != MemberRole.MANAGER && requesterList.getRole() != MemberRole.OWNER) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED);
        }

        MembershipHold hold = holdRepository.findById(request.getHoldPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.HOLD_NOT_FOUND));

        if (hold.getStatus() == HoldStatus.COMPLETED || hold.getStatus() == HoldStatus.CANCELLED) {
            throw new ValidationException(ErrorCode.HOLD_NOT_CANCELLABLE);
        }

        Membership membership = hold.getMembership();
        Member canceller = memberRepository.findById(userDetails.getMemberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        boolean wasActive = hold.getStatus() == HoldStatus.ACTIVE;
        hold.cancel(canceller);

        if (wasActive) {
            membership.resumeActive(hold.getOriginalExpirationDate());
        }

        saveHistory(membership, canceller, HistoryActionType.HOLD_CANCELLED, "홀딩 롤백(취소): holdPk=" + hold.getHoldPk());
    }

    @Transactional(readOnly = true)
    public List<MembershipHoldResponse> getHolds(Long boxPk, Long memberPk, CustomUserDetails userDetails) {
        boolean exists = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(boxPk, userDetails.getMemberPk(), BaseTime.Yn.N);
        if (!exists) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        Membership membership = membershipRepository.findAllMembershipByBoxPkAndMemberPk(boxPk, memberPk)
                .stream().findFirst()
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        return holdRepository.findAllByMembershipMembershipPkOrderByHoldStartDateDesc(membership.getMembershipPk())
                .stream()
                .map(MembershipHoldResponse::from)
                .collect(Collectors.toList());
    }

    // 스케줄러: 오늘부터 시작하는 SCHEDULED 홀딩 → ACTIVE 전환 + 수업 취소
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void activateScheduledHolds() {
        log.info("[SCHEDULE] activateScheduledHolds START");
        LocalDate today = LocalDate.now(KST);

        List<MembershipHold> targets = holdRepository.findAllByStatusAndHoldStartDateLessThanEqual(HoldStatus.SCHEDULED, today);
        for (MembershipHold hold : targets) {
            try {
                hold.activate();
                hold.getMembership().startHolding();
                cancelClassesInHoldPeriod(
                        hold.getMembership().getMember().getMemberPk(),
                        hold.getMembership().getBox().getBoxPk(),
                        hold.getHoldStartDate(),
                        hold.getHoldEndDate());
                log.info("Hold activated: holdPk={}, membershipPk={}", hold.getHoldPk(), hold.getMembership().getMembershipPk());
            } catch (Exception e) {
                log.error("Failed to activate hold: holdPk={}", hold.getHoldPk(), e);
            }
        }
        log.info("[SCHEDULE] activateScheduledHolds END");
    }

    // 스케줄러: holdEndDate 지난 ACTIVE 홀딩 → COMPLETED + 만료일 재계산
    @Scheduled(cron = "0 15 0 * * *", zone = "Asia/Seoul")
    public void completeActiveHolds() {
        log.info("[SCHEDULE] completeActiveHolds START");
        LocalDate today = LocalDate.now(KST);

        List<MembershipHold> targets = holdRepository.findAllByStatusAndHoldEndDateLessThan(HoldStatus.ACTIVE, today);
        for (MembershipHold hold : targets) {
            try {
                LocalDate actualEnd = hold.getHoldEndDate();
                hold.complete(actualEnd);

                // B안: newExpirationDate = actualEndDate + (originalExpirationDate - holdStartDate)
                long remainingDays = ChronoUnit.DAYS.between(hold.getHoldStartDate(), hold.getOriginalExpirationDate());
                LocalDate newExpiration = actualEnd.plusDays(remainingDays);
                hold.getMembership().resumeActive(newExpiration);

                log.info("Hold completed: holdPk={}, newExpiration={}", hold.getHoldPk(), newExpiration);
            } catch (Exception e) {
                log.error("Failed to complete hold: holdPk={}", hold.getHoldPk(), e);
            }
        }
        log.info("[SCHEDULE] completeActiveHolds END");
    }

    private void validateHoldPolicy(Membership membership, LocalDate startDate, LocalDate endDate) {
        if (membership.getHoldDays() == null) {
            throw new ValidationException(ErrorCode.HOLD_NOT_ALLOWED);
        }

        int newHoldDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int usedDays = holdRepository.findAllByMembershipMembershipPkAndStatusIn(membership.getMembershipPk(), USED_HOLD_STATUSES)
                .stream()
                .mapToInt(h -> (int) ChronoUnit.DAYS.between(h.getHoldStartDate(), h.getHoldEndDate()) + 1)
                .sum();

        if (usedDays + newHoldDays > membership.getHoldDays()) {
            throw new ValidationException(ErrorCode.HOLD_DAYS_EXCEEDED);
        }
    }

    private void cancelClassesInHoldPeriod(Long memberPk, Long boxPk, LocalDate startDate, LocalDate endDate) {
        classParticipantRepository
                .findActiveByMemberAndBoxAndDateRange(memberPk, boxPk, startDate, endDate)
                .forEach(BaseTime::markDeleted);
    }

    private void saveHistory(Membership membership, Member actor, HistoryActionType actionType, String memo) {
        try {
            String snapshot = objectMapper.writeValueAsString(MembershipHistorySnapshot.from(membership));
            MembershipHistory history = MembershipHistory.builder()
                    .membership(membership)
                    .actionType(actionType)
                    .preValue(null)
                    .newValue(snapshot)
                    .memo(memo)
                    .period(0)
                    .createdBy(actor)
                    .build();
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("Failed to save hold history. membershipPk={}", membership.getMembershipPk(), e);
        }
    }

    private void notifyManagers(Long boxPk, Long memberPk, Membership membership) {
        try {
            List<Long> managerPks = memberListRepository.findMemberPksByBoxPkAndRoles(
                    boxPk,
                    List.of(MemberRole.MANAGER, MemberRole.OWNER),
                    BaseTime.Yn.N);
            managerPks.remove(memberPk);
            pushService.notifyUsers(managerPks,
                    PushDto.builder()
                            .title("멤버십 홀딩 신청")
                            .body(membership.getMember().getNickname() + "님이 홀딩을 신청했습니다.")
                            .build());
        } catch (Exception e) {
            log.error("Failed to notify managers for hold. boxPk={}, memberPk={}", boxPk, memberPk, e);
        }
    }
}
