package com.fitian.burntz.domain.admin.service;

import com.fitian.burntz.domain.admin.dto.response.AdminBoxDetailResponse;
import com.fitian.burntz.domain.admin.dto.response.BoxActivityResponse;
import com.fitian.burntz.domain.article.repository.ArticleRepository;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.repository.BoxActivityRepository;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.box.repository.BoxSubscriptionRepository;
import com.fitian.burntz.domain.box.repository.SubscriptionEventLogRepository;
import com.fitian.burntz.domain.channel.entity.Channel;
import com.fitian.burntz.domain.channel.repository.ChannelParticipantRepository;
import com.fitian.burntz.domain.channel.repository.ChannelRepository;
import com.fitian.burntz.domain.classes.repository.ClassParticipantRepository;
import com.fitian.burntz.domain.classes.repository.ClassesRepository;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.repository.MembershipHistoryRepository;
import com.fitian.burntz.domain.membership.repository.MembershipRepository;
import com.fitian.burntz.domain.record.repository.RecordRepository;
import com.fitian.burntz.domain.wod.repository.WodRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBoxService {

    private final BoxActivityRepository boxActivityRepository;
    private final BoxRepository boxRepository;
    private final BoxSubscriptionRepository boxSubscriptionRepository;
    private final SubscriptionEventLogRepository subscriptionEventLogRepository;
    private final MemberRepository memberRepository;
    private final MemberListRepository memberListRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipHistoryRepository membershipHistoryRepository;
    private final WodRepository wodRepository;
    private final ClassesRepository classesRepository;
    private final ClassParticipantRepository classParticipantRepository;
    private final RecordRepository recordRepository;
    private final ChannelRepository channelRepository;
    private final ChannelParticipantRepository channelParticipantRepository;
    private final ArticleRepository articleRepository;

    public AdminBoxDetailResponse getBoxDetail(Long boxPk) {
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
        List<Long> memberPks = memberLists.stream().map(ml -> ml.getMember().getMemberPk()).toList();

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

        return AdminBoxDetailResponse.builder()
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
    }

    public List<AdminBoxDetailResponse.MembershipWithHistoryInfo> getMemberMemberships(Long boxPk, Long memberPk) {
        List<Membership> memberships = membershipRepository.findByBoxBoxPkAndMemberMemberPk(boxPk, memberPk);

        return memberships.stream()
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
    }

    public AdminBoxDetailResponse.WodDayInfo getWodDay(Long boxPk, LocalDate date) {
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

        return AdminBoxDetailResponse.WodDayInfo.builder()
                .wod(wodInfo)
                .records(records)
                .build();
    }

    public List<AdminBoxDetailResponse.ClassInfo> getBoxClasses(Long boxPk, LocalDate start, LocalDate end) {
        boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        LocalDate resolvedStart = start != null ? start : LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate resolvedEnd = end != null ? end : resolvedStart.plusDays(6);

        return classesRepository
                .findWithParticipantCountByBoxAndDate(boxPk, resolvedStart, resolvedEnd, BaseTime.Yn.N, BaseTime.Yn.N)
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
    }

    public List<AdminBoxDetailResponse.ClassParticipantInfo> getClassParticipants(Long classesPk) {
        return classParticipantRepository.findByClassesPkWithMemberList(classesPk, BaseTime.Yn.N)
                .stream()
                .map(cp -> AdminBoxDetailResponse.ClassParticipantInfo.builder()
                        .memberPk(cp.getMemberList().getMember().getMemberPk())
                        .boxNickname(cp.getMemberList().getBoxNickname())
                        .role(cp.getMemberList().getRole().name())
                        .build())
                .toList();
    }

    public List<AdminBoxDetailResponse.ChannelInfo> getBoxChannels(Long boxPk) {
        boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        List<Channel> channels = channelRepository.findAllByBoxBoxPkAndDeletedYNOrderByChannelPkAsc(boxPk, BaseTime.Yn.N);

        if (channels.isEmpty()) return List.of();

        List<Long> channelPks = channels.stream().map(Channel::getChannelPk).toList();
        Map<Long, Long> participantCountMap = channelParticipantRepository
                .countActiveParticipantsGroupByChannelPk(channelPks)
                .stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        return channels.stream()
                .map(c -> AdminBoxDetailResponse.ChannelInfo.builder()
                        .channelPk(c.getChannelPk())
                        .channelName(c.getChannelName())
                        .channelEmoji(c.getChannelEmoji())
                        .channelType(c.getChannelType().name())
                        .participantCount(participantCountMap.getOrDefault(c.getChannelPk(), 0L).intValue())
                        .build())
                .toList();
    }

    public List<AdminBoxDetailResponse.RecordInfo> getBoxRecords(Long boxPk) {
        boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(60);

        return recordRepository.findByBoxPkAndWodDateBetweenAndDeletedYN(boxPk, start, end, BaseTime.Yn.N)
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
    }

    public Page<BoxActivityResponse> getBoxActivities(Long boxPk, int page, int size) {
        int safeSize = Math.min(size, 100);
        return boxActivityRepository
                .findByBoxPkOrderByCreatedAtDesc(boxPk, PageRequest.of(page, safeSize))
                .map(BoxActivityResponse::from);
    }

    /**
     * [테스트 박스 전용 하드딜리트]
     * 테스트 목적으로 생성된 박스를 완전히 제거하기 위한 어드민 전용 기능입니다.
     * 박스에 속한 WOD·기록·클래스·채널·멤버십 등 모든 데이터를 영구 삭제합니다.
     * 회원(Member) 계정은 삭제하지 않습니다.
     * 운영 중인 실제 박스에는 절대 사용하지 마십시오.
     */
    @Transactional
    public void deleteBox(Long boxPk) {
        boxRepository.findById(boxPk)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

        recordRepository.deleteAllByBoxPk(boxPk);
        wodRepository.deleteAllByBoxPk(boxPk);
        classParticipantRepository.deleteAllByBoxPk(boxPk);
        classesRepository.deleteAllByBoxPk(boxPk);
        channelParticipantRepository.deleteAllByBoxPk(boxPk);
        channelRepository.deleteAllByBoxPk(boxPk);
        membershipHistoryRepository.deleteAllByBoxPk(boxPk);
        membershipRepository.deleteAllByBoxPk(boxPk);
        memberRepository.reassignLastVisitedBoxPk(boxPk);
        memberListRepository.deleteAllByBoxPk(boxPk);
        boxActivityRepository.deleteAllByBoxPk(boxPk);
        subscriptionEventLogRepository.deleteAllByBoxPk(boxPk);
        boxSubscriptionRepository.deleteAllByBoxPk(boxPk);
        articleRepository.deleteAllByBoxPk(boxPk);
        boxRepository.deleteById(boxPk);
    }
}
