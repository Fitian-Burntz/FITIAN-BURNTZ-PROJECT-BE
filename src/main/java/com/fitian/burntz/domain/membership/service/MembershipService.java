package com.fitian.burntz.domain.membership.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitian.burntz.domain.alarm.service.PushService;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.ActivityType;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.event.BoxActivityEvent;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.channel.service.ChannelService;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.membership.entity.MembershipHistory;
import com.fitian.burntz.domain.membership.enums.HistoryActionType;
import com.fitian.burntz.domain.membership.enums.MembershipStatus;
import com.fitian.burntz.domain.membership.enums.HoldStatus;
import com.fitian.burntz.domain.membership.repository.BoxHoldingPolicyRepository;
import com.fitian.burntz.domain.membership.repository.MembershipHistoryRepository;
import com.fitian.burntz.domain.membership.repository.MembershipHoldRepository;
import com.fitian.burntz.domain.membership.v1.dto.*;
import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.repository.MembershipRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.membership.service
 * @fileName : MembershipService
 * @date : 2025-09-17
 * @description : 멤버십 서비스입니다.
 */

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MembershipService {

    private static final List<HoldStatus> USED_HOLD_STATUSES = List.of(HoldStatus.ACTIVE, HoldStatus.COMPLETED);

    private final ObjectMapper objectMapper;
    private final ChannelService channelService;
    private final MemberRepository memberRepository;
    private final BoxRepository boxRepository;
    private final MembershipRepository membershipRepository;
    private final MemberListRepository memberListRepository;
    private final MembershipHistoryRepository historyRepository;
    private final MembershipHoldRepository holdRepository;
    private final BoxHoldingPolicyRepository policyRepository;
    private final PushService pushService;
    private final ApplicationEventPublisher eventPublisher;

    public MembershipResponse getMembership(Long boxPk, Long memberPk, CustomUserDetails userDetails) {
        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) {
           memberPk = userDetails.getMemberPk();
        }

        Membership membership = membershipRepository.findAllMembershipByBoxPkAndMemberPk(boxPk, memberPk)
                .stream().findFirst()
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERSHIP_NOT_FOUND));
        MemberList target = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(memberPk, boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        int usedHoldDays = holdRepository
                .findAllByMembershipMembershipPkAndStatusIn(membership.getMembershipPk(), USED_HOLD_STATUSES)
                .stream()
                .mapToInt(h -> (int) ChronoUnit.DAYS.between(h.getHoldStartDate(), h.getHoldEndDate()) + 1)
                .sum();

        return MembershipResponse.builder()
                .membershipPk(membership.getMembershipPk())
                .membershipName(membership.getMembershipName())
                .startDate(membership.getStartDate())
                .expirationDate(membership.getExpirationDate())
                .status(membership.getStatus())
                .memo(membership.getMemo())
                .boxPk(membership.getBox().getBoxPk())
                .boxNickname(target.getBoxNickname())
                .holdDays(membership.getHoldDays())
                .usedHoldDays(usedHoldDays)
                .build();
    }

    public void createMembership(Long boxPk, Long memberPk, MembershipCreateRequest request, CustomUserDetails userDetails) {

        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        Member member = memberRepository.findById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        Box box = boxRepository.findById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));
        MemberList memberList = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(memberPk, boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        boolean exists = membershipRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYNAndStatus(boxPk, memberPk, BaseTime.Yn.N, MembershipStatus.ACTIVE);
        if(exists) throw new ValidationException(ErrorCode.DUPLICATE_MEMBERSHIP);

        // 기존 만료/삭제 멤버십 soft-delete (새 멤버십과 중복 조회 방지)
        membershipRepository.findAllMembershipByBoxPkAndMemberPk(boxPk, memberPk)
                .forEach(BaseTime::markDeleted);

        Integer holdDays = request.getHoldDays() != null
                ? request.getHoldDays()
                : policyRepository.findByBoxBoxPk(boxPk)
                        .map(p -> p.getDefaultHoldDays())
                        .orElse(null);

        Membership membership = Membership.builder()
                .membershipName(request.getMembershipName())
                .startDate(request.getStartDate())
                .expirationDate(request.getExpirationDate())
                .status(request.getStatus())
                .memo(request.getMemo())
                .holdDays(holdDays)
                .member(member)
                .box(box)
                .build();

        // 멤버 role 변경
        memberList.changeRole(MemberRole.MEMBER);
        memberListRepository.save(memberList);

        // 퍼플릭 채널 초대
        channelService.inviteMemberToAllPublicChannels(memberPk, boxPk);

        membershipRepository.save(membership);

        try {
            Member createdBy = memberRepository.findById(userDetails.getMemberPk())
                    .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

            String newValue = objectMapper.writeValueAsString(MembershipHistorySnapshot.from(membership));
            MembershipHistory history = MembershipHistory.builder()
                    .membership(membership)
                    .actionType(HistoryActionType.CREATE)
                    .preValue(null)
                    .newValue(newValue)
                    .memo("CREATOR : "+list.getBoxNickname()+"( "+list.getRole()+" )"+request.getMemo())
                    .period(request.getPeriod())
                    .createdBy(createdBy)
                    .build();
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("Failed to save Membership History. membershipPk={}, boxPk={}, memberPk={}",
                    membership.getMembershipPk(), boxPk, memberPk, e);
            throw new RuntimeException("Failed to save Membership History.", e);
        }

        eventPublisher.publishEvent(BoxActivityEvent.withTarget(
                boxPk, ActivityType.MEMBERSHIP_CREATED,
                userDetails.getMemberPk(), list.getBoxNickname(),
                memberPk, member.getNickname(), null
        ));
    }

    public void updateMembership(Long boxPk, Long memberPk, MembershipUpdateRequest request, CustomUserDetails userDetails){
        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        Membership membership = membershipRepository.findById(request.getMembershipPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        if(!memberPk.equals(membership.getMember().getMemberPk())) throw new ValidationException(ErrorCode.USER_NOT_FOUND);

        try {
            String preValue = objectMapper.writeValueAsString(MembershipHistorySnapshot.from(membership));
            membership.updateFrom(request);
            String newValue = objectMapper.writeValueAsString(MembershipHistorySnapshot.from(membership));

            Member createdBy = memberRepository.findById(userDetails.getMemberPk())
                    .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

            MembershipHistory history = MembershipHistory.builder()
                    .membership(membership)
                    .actionType(HistoryActionType.UPDATE)
                    .preValue(preValue)
                    .newValue(newValue)
                    .memo("CREATOR : "+list.getBoxNickname()+"( "+list.getRole()+" )"+request.getMemo())
                    .period(request.getPeriod())
                    .createdBy(createdBy)
                    .build();
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("Failed to save Membership History. membershipPk={}, boxPk={}, memberPk={}",
                    membership.getMembershipPk(), boxPk, memberPk, e);
            throw new RuntimeException("Failed to save Membership History.", e);
        }

        eventPublisher.publishEvent(BoxActivityEvent.withTarget(
                boxPk, ActivityType.MEMBERSHIP_EXTENDED,
                userDetails.getMemberPk(), list.getBoxNickname(),
                memberPk, membership.getMember().getNickname(), null
        ));
    }

    public void deleteMembership(Long boxPk, Long memberPk, MembershipIdentifierRequest request, CustomUserDetails userDetails){
        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        Membership membership = membershipRepository.findById(request.getMembershipPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        MemberList ml = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(memberPk, boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        if(!memberPk.equals(membership.getMember().getMemberPk())) throw new ValidationException(ErrorCode.USER_NOT_FOUND);

        try{
            String preValue = objectMapper.writeValueAsString(MembershipHistorySnapshot.from(membership));
            membership.delete();
            membership.markDeleted();
            ml.changeRole(MemberRole.GUEST);
            membershipRepository.flush();
            memberListRepository.flush();
            channelService.removeMemberFromAllPublicChannels(memberPk, boxPk);

            Member createdBy = memberRepository.findById(userDetails.getMemberPk())
                    .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

            MembershipHistory history = MembershipHistory.builder()
                    .membership(membership)
                    .actionType(HistoryActionType.DELETE)
                    .preValue(preValue)
                    .newValue(null)
                    .memo("CREATOR : "+list.getBoxNickname()+"( "+list.getRole()+" )")
                    .period(0)
                    .createdBy(createdBy)
                    .build();
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("Failed to save Membership History. membershipPk={}, boxPk={}, memberPk={}",
                    membership.getMembershipPk(), boxPk, memberPk);
            throw new RuntimeException("Failed to save Membership History.", e);
        }
    }

    public List<MembershipHistoryResponse> getMembershipHistory(Long boxPk, Long memberPk, CustomUserDetails userDetails) {

        //존재하는 회원인지 검증
        boolean exist = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(boxPk, userDetails.getMemberPk(), BaseTime.Yn.N);
        if(!exist) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        boolean exist2 = memberListRepository.existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(boxPk, memberPk, BaseTime.Yn.N);
        if(!exist2) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        //가장 최근 멤버십의 로그만 확인
//        Membership membership = membershipRepository.findLatestByBoxPkAndMemberPk(boxPk, memberPk)
//                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERSHIP_NOT_FOUND));
//
//        List<MembershipHistory> historyList2 = historyRepository.findAllByMembershipMembershipPk(membership.getMembershipPk());

        //이전 멤버십까지 전체 로그 확인
        List<Membership> membershipList = membershipRepository.findByBoxBoxPkAndMemberMemberPk(boxPk, memberPk);
        Set<Long> membershipPkSet = membershipList.stream()
                .map(Membership::getMembershipPk)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<MembershipHistory> historyList = historyRepository.findAllByMembershipMembershipPkIn(membershipPkSet);

        List<MembershipHistoryResponse> responseList = new ArrayList<>();

        for(MembershipHistory mh : historyList) {
            MembershipHistoryResponse response = MembershipHistoryResponse.builder()
                   .membershipPk(mh.getMembership().getMembershipPk())
                   .actionType(mh.getActionType())
                   .preValue(mh.getPreValue())
                   .newValue(mh.getNewValue())
                   .memo(mh.getMemo())
                   .period(mh.getPeriod())
                   .createdBy(mh.getCreatedBy().getMemberPk())
                   .createdAt(mh.getCreatedAt())
                   .build();
           responseList.add(response);
        }
        return responseList;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void checkExpirationDate() {
        log.info("[SCHEDULE] checkExpirationDate START");
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        List<Membership> expiredTargets =
                membershipRepository.findAllByExpirationDateLessThanAndStatusAndDeletedYN(today, MembershipStatus.ACTIVE, BaseTime.Yn.N);
        // HOLDING 상태는 홀딩 스케줄러가 처리하므로 만료 대상에서 제외됨 (이미 ACTIVE만 조회)

        List<MemberList> mlList = new ArrayList<>();

        for (Membership membership : expiredTargets) {
            membership.expire();

            try {
                MemberList ml = memberListRepository
                        .findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(
                                membership.getMember().getMemberPk(),
                                membership.getBox().getBoxPk(),
                                BaseTime.Yn.N
                        )
                        .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

                ml.changeRole(MemberRole.GUEST);
                mlList.add(ml);
                channelService.removeMemberFromAllPublicChannels(membership.getMember().getMemberPk(), membership.getBox().getBoxPk());
                pushService.notifyUserString(membership.getMember().getMemberPk(), "멤버십 만료", membership.getBox().getBoxName()+"의 멤버십이 만료되었습니다.");
                log.info("memberPk = {}, boxPk = {}, membershipPk = {}   -> EXPIRED", ml.getMember().getMemberPk(), ml.getBox().getBoxPk(), membership.getMembershipPk());
            } catch (Exception e) {
                log.error("Failed to process expiration. membershipPk={}", membership.getMembershipPk(), e);
            }
        }

        membershipRepository.saveAll(expiredTargets);
        memberListRepository.saveAll(mlList);
        log.info("[SCHEDULE] checkExpirationDate END");
    }
}
