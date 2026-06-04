package com.fitian.burntz.domain.admin.controller;

import com.fitian.burntz.domain.admin.dto.AdminAccount;
import com.fitian.burntz.domain.admin.dto.response.AdminBoxDetailResponse;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.classes.repository.ClassParticipantRepository;
import com.fitian.burntz.domain.classes.repository.ClassesRepository;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.repository.MembershipHistoryRepository;
import com.fitian.burntz.domain.membership.repository.MembershipRepository;
import com.fitian.burntz.domain.record.repository.RecordRepository;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.repository.WodRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminBoxController {

    private final AdminAccount adminAccount;
    private final BoxRepository boxRepository;
    private final MemberRepository memberRepository;
    private final MemberListRepository memberListRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipHistoryRepository membershipHistoryRepository;
    private final WodRepository wodRepository;
    private final ClassesRepository classesRepository;
    private final ClassParticipantRepository classParticipantRepository;
    private final RecordRepository recordRepository;

    @GetMapping("/boxes/{boxPk}/detail")
    public ApiResponse<AdminBoxDetailResponse> getBoxDetail(
            @PathVariable Long boxPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 박스 상세 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(null);
        }

        Box box = boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        AdminBoxDetailResponse.OwnerInfo ownerInfo = memberRepository.findActiveById(box.getOwnerPk())
                .map(owner -> AdminBoxDetailResponse.OwnerInfo.builder()
                        .memberPk(owner.getMemberPk())
                        .nickname(owner.getNickname())
                        .email(owner.getEmail())
                        .build())
                .orElse(null);

        List<MemberList> memberLists = memberListRepository.findAllByBoxAndDeletedYN(box, BaseTime.Yn.N);

        List<Long> memberPks = memberLists.stream()
                .map(ml -> ml.getMember().getMemberPk())
                .toList();

        Map<Long, Membership> membershipMap = memberPks.isEmpty()
                ? Map.of()
                : membershipRepository.findLatestMembershipPerMemberByBox(boxPk, memberPks).stream()
                        .collect(Collectors.toMap(m -> m.getMember().getMemberPk(), m -> m));

        List<AdminBoxDetailResponse.MemberInfo> memberInfos = memberLists.stream()
                .map(ml -> {
                    Membership ms = membershipMap.get(ml.getMember().getMemberPk());
                    return AdminBoxDetailResponse.MemberInfo.builder()
                            .memberPk(ml.getMember().getMemberPk())
                            .boxNickname(ml.getBoxNickname())
                            .role(ml.getRole().name())
                            .email(ml.getMember().getEmail())
                            .membershipName(ms != null ? ms.getMembershipName() : null)
                            .membershipStatus(ms != null ? ms.getStatus().name() : null)
                            .startDate(ms != null ? ms.getStartDate() : null)
                            .expirationDate(ms != null ? ms.getExpirationDate() : null)
                            .build();
                })
                .toList();

        AdminBoxDetailResponse response = AdminBoxDetailResponse.builder()
                .boxPk(box.getBoxPk())
                .boxName(box.getBoxName())
                .boxCode(box.getBoxCode())
                .boxAddress(box.getBoxAddress())
                .boxContact(box.getBoxContact())
                .subscribeStatus(box.getSubscribe().name())
                .owner(ownerInfo)
                .memberCount(memberInfos.size())
                .members(memberInfos)
                .build();

        return ApiResponse.success(response);
    }

    @GetMapping("/boxes/{boxPk}/members/{memberPk}/memberships")
    public ApiResponse<List<AdminBoxDetailResponse.MembershipWithHistoryInfo>> getMemberMemberships(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 멤버십 히스토리 조회 불가 (boxPk={}, memberPk={})", boxPk, memberPk);
            return ApiResponse.success(List.of());
        }

        List<Membership> memberships = membershipRepository.findByBoxBoxPkAndMemberMemberPk(boxPk, memberPk);

        List<AdminBoxDetailResponse.MembershipWithHistoryInfo> result = memberships.stream()
                .map(ms -> {
                    List<AdminBoxDetailResponse.HistoryEntry> histories =
                            membershipHistoryRepository.findAllByMembershipPkWithCreator(ms.getMembershipPk())
                                    .stream()
                                    .map(h -> AdminBoxDetailResponse.HistoryEntry.builder()
                                            .historyPk(h.getMembershipHistoryPk())
                                            .actionType(h.getActionType().name())
                                            .preValue(h.getPreValue())
                                            .newValue(h.getNewValue())
                                            .memo(h.getMemo())
                                            .period(h.getPeriod())
                                            .createdByNickname(h.getCreatedBy().getNickname())
                                            .createdAt(h.getCreatedAt())
                                            .build())
                                    .toList();

                    return AdminBoxDetailResponse.MembershipWithHistoryInfo.builder()
                            .membershipPk(ms.getMembershipPk())
                            .membershipName(ms.getMembershipName())
                            .status(ms.getStatus().name())
                            .startDate(ms.getStartDate())
                            .expirationDate(ms.getExpirationDate())
                            .memo(ms.getMemo())
                            .histories(histories)
                            .build();
                })
                .sorted((a, b) -> Long.compare(b.getMembershipPk(), a.getMembershipPk()))
                .toList();

        return ApiResponse.success(result);
    }

    @GetMapping("/boxes/{boxPk}/wod-day")
    public ApiResponse<AdminBoxDetailResponse.WodDayInfo> getWodDay(
            @PathVariable Long boxPk,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - WOD 날짜별 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(null);
        }

        boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        AdminBoxDetailResponse.WodInfo wodInfo = wodRepository
                .findByBoxBoxPkAndWodDateAndDeletedYN(boxPk, date, BaseTime.Yn.N)
                .map(w -> AdminBoxDetailResponse.WodInfo.builder()
                        .wodPk(w.getWodPk())
                        .wodTitle(w.getWodTitle())
                        .wodType(w.getWodType() != null ? w.getWodType().name() : null)
                        .wodDate(w.getWodDate())
                        .wodScript(w.getWodScript())
                        .build())
                .orElse(null);

        List<AdminBoxDetailResponse.RecordInfo> records = recordRepository
                .findByBoxPkAndWodDateBetweenAndDeletedYN(boxPk, date, date, BaseTime.Yn.N)
                .stream()
                .map(r -> AdminBoxDetailResponse.RecordInfo.builder()
                        .recordPk(r.getRecordPk())
                        .nickname(r.getNickname() != null ? r.getNickname()
                                : (r.getMemberList() != null ? r.getMemberList().getBoxNickname() : null))
                        .level(r.getLevel())
                        .round(r.getRound())
                        .reps(r.getReps())
                        .time(r.getTime())
                        .result(r.getResult() != null ? r.getResult().name() : null)
                        .memo(r.getMemo())
                        .wodDate(r.getWod().getWodDate())
                        .wodTitle(r.getWod().getWodTitle())
                        .wodType(r.getWod().getWodType() != null ? r.getWod().getWodType().name() : null)
                        .build())
                .toList();

        return ApiResponse.success(AdminBoxDetailResponse.WodDayInfo.builder()
                .wod(wodInfo)
                .records(records)
                .build());
    }

    @GetMapping("/boxes/{boxPk}/classes")
    public ApiResponse<List<AdminBoxDetailResponse.ClassInfo>> getBoxClasses(
            @PathVariable Long boxPk,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 클래스 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(List.of());
        }

        boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        if (start == null) start = LocalDate.now().with(DayOfWeek.MONDAY);
        if (end == null) end = start.plusDays(6);

        List<AdminBoxDetailResponse.ClassInfo> classes = classesRepository
                .findWithParticipantCountByBoxAndDate(boxPk, start, end, BaseTime.Yn.N, BaseTime.Yn.N)
                .stream()
                .map(c -> AdminBoxDetailResponse.ClassInfo.builder()
                        .classesPk(c.getClasses().getClassesPk())
                        .classTitle(c.getClasses().getClassTitle())
                        .classDate(c.getClasses().getClassDate())
                        .startTime(c.getClasses().getStartTime())
                        .endTime(c.getClasses().getEndTime())
                        .capacity(c.getClasses().getClassMemberCapacity())
                        .participantCount(c.getParticipantCount())
                        .classMemo(c.getClasses().getClassMemo())
                        .build())
                .toList();

        return ApiResponse.success(classes);
    }

    @GetMapping("/boxes/{boxPk}/classes/{classesPk}/participants")
    public ApiResponse<List<AdminBoxDetailResponse.ClassParticipantInfo>> getClassParticipants(
            @PathVariable Long boxPk,
            @PathVariable Long classesPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 참여자 조회 불가 (classesPk={})", classesPk);
            return ApiResponse.success(List.of());
        }

        List<AdminBoxDetailResponse.ClassParticipantInfo> participants =
                classParticipantRepository.findByClassesPkWithMemberList(classesPk, BaseTime.Yn.N)
                        .stream()
                        .map(cp -> AdminBoxDetailResponse.ClassParticipantInfo.builder()
                                .memberPk(cp.getMemberList().getMember().getMemberPk())
                                .boxNickname(cp.getMemberList().getBoxNickname())
                                .role(cp.getMemberList().getRole().name())
                                .build())
                        .toList();

        return ApiResponse.success(participants);
    }

    @GetMapping("/boxes/{boxPk}/records")
    public ApiResponse<List<AdminBoxDetailResponse.RecordInfo>> getBoxRecords(
            @PathVariable Long boxPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 레코드 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(List.of());
        }

        boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(60);

        List<AdminBoxDetailResponse.RecordInfo> records = recordRepository
                .findByBoxPkAndWodDateBetweenAndDeletedYN(boxPk, start, end, BaseTime.Yn.N)
                .stream()
                .map(r -> AdminBoxDetailResponse.RecordInfo.builder()
                        .recordPk(r.getRecordPk())
                        .nickname(r.getNickname() != null ? r.getNickname()
                                : (r.getMemberList() != null ? r.getMemberList().getBoxNickname() : null))
                        .level(r.getLevel())
                        .round(r.getRound())
                        .reps(r.getReps())
                        .time(r.getTime())
                        .result(r.getResult() != null ? r.getResult().name() : null)
                        .memo(r.getMemo())
                        .wodDate(r.getWod().getWodDate())
                        .wodTitle(r.getWod().getWodTitle())
                        .wodType(r.getWod().getWodType() != null ? r.getWod().getWodType().name() : null)
                        .build())
                .toList();

        return ApiResponse.success(records);
    }
}
