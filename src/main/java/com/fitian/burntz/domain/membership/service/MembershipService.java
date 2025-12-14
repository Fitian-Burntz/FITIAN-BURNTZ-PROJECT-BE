package com.fitian.burntz.domain.membership.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.membership.entity.MembershipHistory;
import com.fitian.burntz.domain.membership.enums.HistoryActionType;
import com.fitian.burntz.domain.membership.repository.MembershipHistoryRepository;
import com.fitian.burntz.domain.membership.v1.dto.*;
import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.repository.MembershipRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;
    private final BoxRepository boxRepository;
    private final MembershipRepository membershipRepository;
    private final MemberListRepository memberListRepository;
    private final MembershipHistoryRepository historyRepository;

    public MembershipResponse getMembership(Long boxPk, Long memberPk, CustomUserDetails userDetails) {
        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) {
           memberPk = userDetails.getMemberPk();
        }

        Membership membership = membershipRepository.findByBoxBoxPkAndMemberMemberPkAndDeletedYN(boxPk, memberPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERSHIP_NOT_FOUND));
        MemberList target = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(memberPk, boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        return MembershipResponse.builder()
                .membershipPk(membership.getMembershipPk())
                .membershipName(membership.getMembershipName())
                .startDate(membership.getStartDate())
                .expirationDate(membership.getExpirationDate())
                .status(membership.getStatus())
                .memo(membership.getMemo())
                .boxPk(membership.getBox().getBoxPk())
                .boxNickname(target.getBoxNickname())
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

        Membership membership = Membership.builder()
                .membershipName(request.getMembershipName())
                .startDate(request.getStartDate())
                .expirationDate(request.getExpirationDate())
                .status(request.getStatus())
                .memo(request.getMemo())
                .member(member)
                .box(box)
                .build();

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
                    .memo(request.getMemo())
                    .period(request.getPeriod())
                    .createdBy(createdBy)
                    .build();
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("Failed to save Membership History. membershipPk={}, boxPk={}, memberPk={}",
                    membership.getMembershipPk(), boxPk, memberPk, e);
            throw new RuntimeException("Failed to save Membership History.", e);
        }
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
                    .memo(request.getMemo())
                    .period(request.getPeriod())
                    .createdBy(createdBy)
                    .build();
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("Failed to save Membership History. membershipPk={}, boxPk={}, memberPk={}",
                    membership.getMembershipPk(), boxPk, memberPk, e);
            throw new RuntimeException("Failed to save Membership History.", e);
        }
    }

    public void deleteMembership(Long boxPk, Long memberPk, MembershipIdentifierRequest request, CustomUserDetails userDetails){
        //회원 등급 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        Membership membership = membershipRepository.findById(request.getMembershipPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        if(!memberPk.equals(membership.getMember().getMemberPk())) throw new ValidationException(ErrorCode.USER_NOT_FOUND);

        try{
            String preValue = objectMapper.writeValueAsString(MembershipHistorySnapshot.from(membership));
            membership.markDeleted();

            Member createdBy = memberRepository.findById(userDetails.getMemberPk())
                    .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

            MembershipHistory history = MembershipHistory.builder()
                    .membership(membership)
                    .actionType(HistoryActionType.DELETE)
                    .preValue(preValue)
                    .newValue(null)
                    .memo(null)
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

        Membership membership = membershipRepository.findLatestByBoxPkAndMemberPk(boxPk, memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERSHIP_NOT_FOUND));

        List<MembershipHistory> historyList = historyRepository.findAllByMembershipMembershipPk(membership.getMembershipPk());

        List<MembershipHistoryResponse> responseList = new ArrayList<>();

        for(MembershipHistory mh : historyList) {
            MembershipHistoryResponse response = MembershipHistoryResponse.builder()
                   .membershipPk(mh.getMembershipHistoryPk())
                   .actionType(mh.getActionType())
                   .preValue(mh.getPreValue())
                   .newValue(mh.getNewValue())
                   .memo(mh.getMemo())
                   .period(mh.getPeriod())
                   .createdBy(mh.getCreatedBy())
                   .createdAt(mh.getCreatedAt())
                   .build();
           responseList.add(response);
        }
        return responseList;
    }
}
