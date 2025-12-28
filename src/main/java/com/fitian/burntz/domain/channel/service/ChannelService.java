package com.fitian.burntz.domain.channel.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.channel.entity.ChannelParticipant;
import com.fitian.burntz.domain.channel.repository.ChannelParticipantRepository;
import com.fitian.burntz.domain.channel.v1.dto.*;
import com.fitian.burntz.domain.channel.entity.Channel;
import com.fitian.burntz.domain.channel.repository.ChannelRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.service
 * @fileName : ChannelService
 * @date : 2025-09-08
 * @description : 채널(채팅) 서비스 입니다.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final BoxRepository boxRepository;
    private final MemberRepository memberRepository;
    private final ChannelParticipantRepository participantRepository;
    private final MemberListRepository memberListRepository;
    private final Firestore firestore;

    public void createChannel(ChannelCreateRequest request, CustomUserDetails userDetails) {

        Box box = boxRepository.findByBoxCode(request.getBoxCode())
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));
        Member creator = memberRepository.findById(userDetails.getMemberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        Channel channel = Channel.builder()
                .channelId(request.getChannelId())
                .channelName(request.getChannelName())
                .channelType(request.getType())
                .box(box)
                .createdBy(creator)
                .build();

        Channel saved = channelRepository.save(channel);

        //Firebase에 채널 생성
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("channelName", request.getChannelName());
            data.put("type", request.getType());
            data.put("memberPks", request.getMemberPks());
            data.put("createdBy",userDetails.getMemberPk());
            data.put("createdAt", com.google.cloud.Timestamp.now());

            firestore.collection("boxes")
                    .document(request.getBoxCode())
                    .collection("channels")
                    .document(request.getChannelId())
                    .set(data);
        } catch (Exception e) {
            throw new RuntimeException("Firestore 채널 문서 생성 실패", e);
        }

        List<Member> memberList = memberRepository.findAllById(request.getMemberPks());

        List<ChannelParticipant> CPList = memberList.stream()
                .map(m -> ChannelParticipant.builder()
                    .channel(saved)
                    .member(m).build())
                .toList();

        participantRepository.saveAll(CPList);
    }

    public List<ChannelListResponse> getChannels(CustomUserDetails userDetails, Long boxPk) {
        Member member = memberRepository.findById(userDetails.getMemberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        Box box = boxRepository.findById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        return participantRepository.findChannelsByMemberAndBox(member, box)
                .stream()
                .map(c -> ChannelListResponse.builder()
                        .channelPk(c.getChannelPk())
                        .channelId(c.getChannelId())
                        .channelName(c.getChannelName())
                        .type(c.getChannelType())
                        .build())
                .collect(Collectors.toList());
    }

    public List<ChannelParticipant> getParticipants(CustomUserDetails userDetails, Long channelPk) {
        Channel channel = channelRepository.findById(channelPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.CHANNEL_NOT_FOUND));

        Member member = memberRepository.findById(userDetails.getMemberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        boolean isParticipated = participantRepository.existsByMemberAndChannel_ChannelPkAndDeletedYN(member, channelPk, BaseTime.Yn.N);

        if(!isParticipated) throw new ValidationException(ErrorCode.FORBIDDEN);

        return participantRepository.findByChannel(channel);
    }

    public void inviteParticipants(ChannelInviteRequest request, CustomUserDetails userDetails) {

        Channel channel = channelRepository.findById(request.getChannelPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.CHANNEL_NOT_FOUND));

        //해당 채널의 참여자 목록 Pk
        List<Long> participantsPk = participantRepository.findMemberPksByChannel(channel);

        List<Long> toInsert = request.getMemberPks()
                .stream().filter(memberPk -> !participantsPk.contains(memberPk))
                .toList();

        List<ChannelParticipant> insertList = new ArrayList<>();

        for(Long memberPk : toInsert) {
            Member member = memberRepository.findById(memberPk)
                    .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
            ChannelParticipant p = ChannelParticipant.builder()
                    .channel(channel)
                    .member(member)
                    .build();
            insertList.add(p);
        }

        participantRepository.saveAll(insertList);

        try {
            firestore.collection("boxes")
                    .document(channel.getBox().getBoxCode())
                    .collection("channels")
                    .document(channel.getChannelId())
                    .update("memberPks",com.google.cloud.firestore.FieldValue.arrayUnion(toInsert.toArray()));
        } catch ( Exception e) {
            throw new RuntimeException("Firestore 참여자 추가 실패", e);
        }
    }

    public boolean canEnterChannel(Long channelPk, CustomUserDetails userDetails) {
        return participantRepository.existByChannelPkAndMemberPkAndDeletedYN(userDetails.getMemberPk(), channelPk, BaseTime.Yn.N);
    }

    public List<ParticipantListResponse> getParticipantsInfo(Long channelPk, CustomUserDetails userDetails){

        Channel channel = channelRepository.findById(channelPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.CHANNEL_NOT_FOUND));

        List<Long> memberPkList = participantRepository.findMemberPksByChannel(channel);
        List<MemberList> memberLists = memberListRepository.findAllByMemberMemberPkInAndBoxBoxPkAndDeletedYN(memberPkList, channel.getBox().getBoxPk(), BaseTime.Yn.N);

        List<ParticipantListResponse> pList = new ArrayList<>();
        for(MemberList ml : memberLists) {
            ParticipantListResponse p = ParticipantListResponse.builder()
                    .memberPk(ml.getMember().getMemberPk())
                    .memberListPk(ml.getMemberListPk())
                    .role(ml.getRole())
                    .boxNickname(ml.getBoxNickname()).build();
            pList.add(p);
        }

        return pList;
    }

    public boolean deleteParticipant(ChannelLeaveRequest request, CustomUserDetails userDetails) {

        Channel channel = channelRepository.findById(request.getChannelPk())
                        .orElseThrow(() -> new ValidationException(ErrorCode.CHANNEL_NOT_FOUND));

        Long boxPk = channel.getBox().getBoxPk();
        String boxCode = channel.getBox().getBoxCode();
        String channelId = channel.getChannelId();

        MemberList ml = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        //내보내기는 OWNER, MANAGER 만 가능
        if(!(ml.getRole() == MemberRole.OWNER || ml.getRole() == MemberRole.MANAGER)) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }

        int updated = participantRepository.markDeletedByPk(request.getChannelPk(), request.getMemberPk(), BaseTime.Yn.Y);

        try {
            firestore.collection("boxes")
                    .document(boxCode)
                    .collection("channels")
                    .document(channelId)
                    .update("memberPks",com.google.cloud.firestore.FieldValue.arrayRemove(request.getMemberPk()));
        } catch ( Exception e) {
            throw new RuntimeException("Firestore 참여자 제거 실패", e);
        }

        return updated > 0;
    }

    public boolean deleteChannel(ChannelDeleteRequest request, CustomUserDetails userDetails) {

        Channel channel = channelRepository.findById(request.getChannelPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.CHANNEL_NOT_FOUND));

        //MANAGER, OWNER 검증
        MemberList list = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(userDetails.getMemberPk(), channel.getBox().getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        if(list.getRole() == MemberRole.GUEST || list.getRole() == MemberRole.MEMBER) throw new ValidationException(ErrorCode.ACCESS_DENIED);

        //해당 채널 참여자 삭제
        int updatedParticipant = participantRepository.markDeletedByChannelPk(request.getChannelPk(), BaseTime.Yn.Y);

        //해당 채널 삭제
        int updatedChannel = channelRepository.markDeletedByChannelPk(request.getChannelPk(), BaseTime.Yn.Y);

        return updatedChannel > 0 && updatedParticipant > 0;
    }
}
