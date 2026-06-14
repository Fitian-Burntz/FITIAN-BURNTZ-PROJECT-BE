package com.fitian.burntz.domain.channel.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.ActivityType;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.event.BoxActivityEvent;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.channel.entity.ChannelParticipant;
import com.fitian.burntz.domain.channel.enums.ChannelType;
import com.fitian.burntz.domain.channel.repository.ChannelParticipantRepository;
import com.fitian.burntz.domain.alarm.service.AlarmService;
import com.fitian.burntz.domain.alarm.v1.dto.MessagePushRequest;
import com.fitian.burntz.domain.channel.v1.dto.*;
import com.fitian.burntz.domain.channel.v2.dto.MessageSendRequest;
import com.fitian.burntz.domain.channel.v2.dto.MessageSendResponse;
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
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.WriteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ApplicationEventPublisher eventPublisher;
    private final AlarmService alarmService;

    public Long createChannel(ChannelCreateRequest request, CustomUserDetails userDetails) {

        // 이미 활성화된 채널이 있으면 기존 channelPk 반환 (DM 멱등성 보장)
        Optional<Channel> existing = channelRepository.findByChannelIdAndDeletedYN(request.getChannelId(), BaseTime.Yn.N);
        if (existing.isPresent()) {
            return existing.get().getChannelPk();
        }

        Box box = boxRepository.findByBoxCode(request.getBoxCode())
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        MemberList ml = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(
                        userDetails.getMemberPk(), box.getBoxPk(), BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        if (request.getType() == ChannelType.DM) {
            if (request.getMemberPks().size() != 2) {
                throw new ValidationException(ErrorCode.INVALID_REQUEST);
            }
        } else {
            if (ml.getRole() != MemberRole.OWNER && ml.getRole() != MemberRole.MANAGER) {
                throw new ValidationException(ErrorCode.FORBIDDEN);
            }
        }

        Member creator = memberRepository.findById(userDetails.getMemberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        List<Member> memberList = memberRepository.findAllById(request.getMemberPks());
        if (memberList.size() != request.getMemberPks().size()) {
            throw new ValidationException(ErrorCode.USER_NOT_FOUND);
        }

        Channel channel = Channel.builder()
                .channelId(request.getChannelId())
                .channelName(request.getChannelName())
                .channelEmoji(request.getChannelEmoji())
                .channelType(request.getType())
                .box(box)
                .createdBy(creator)
                .build();

        Channel saved = channelRepository.save(channel);

        try {
            Map<String, Object> data = new HashMap<>();
            data.put("channelName", request.getChannelName());
            data.put("channelEmoji", request.getChannelEmoji());
            data.put("type", request.getType());
            data.put("memberPks", request.getMemberPks());
            data.put("createdBy", userDetails.getMemberPk());
            data.put("createdAt", com.google.cloud.Timestamp.now());

            firestore.collection("boxes")
                    .document(request.getBoxCode())
                    .collection("channels")
                    .document(request.getChannelId())
                    .set(data);
        } catch (Exception e) {
            throw new RuntimeException("Firestore 채널 문서 생성 실패", e);
        }

        List<ChannelParticipant> CPList = memberList.stream()
                .map(m -> ChannelParticipant.builder()
                    .channel(saved)
                    .member(m).build())
                .toList();

        participantRepository.saveAll(CPList);

        if (request.getType() != ChannelType.NOTICE && request.getType() != ChannelType.GENERAL && request.getType() != ChannelType.DM) {
            eventPublisher.publishEvent(BoxActivityEvent.of(
                    box.getBoxPk(), ActivityType.CHANNEL_CREATED,
                    creator.getMemberPk(), creator.getNickname(),
                    request.getChannelName()
            ));
        }

        return saved.getChannelPk();
    }

    public List<ChannelListResponse> getChannels(CustomUserDetails userDetails, Long boxPk) {
        Member member = memberRepository.findById(userDetails.getMemberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        Box box = boxRepository.findById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        List<Channel> channels = participantRepository.findChannelsByMemberAndBox(member, box);

        List<Channel> dmChannels = channels.stream()
                .filter(c -> c.getChannelType() == ChannelType.DM)
                .toList();

        Map<Long, String> dmProfileMap = new HashMap<>();
        if (!dmChannels.isEmpty()) {
            participantRepository.findPartnerProfileImagesByChannelsAndBox(dmChannels, member, box)
                    .forEach(row -> dmProfileMap.put((Long) row[0], (String) row[1]));
        }

        return channels.stream()
                .map(c -> ChannelListResponse.builder()
                        .channelPk(c.getChannelPk())
                        .channelId(c.getChannelId())
                        .channelEmoji(c.getChannelEmoji())
                        .channelName(c.getChannelName())
                        .type(c.getChannelType())
                        .dmPartnerProfileImageUrl(c.getChannelType() == ChannelType.DM ? dmProfileMap.get(c.getChannelPk()) : null)
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

        boolean exist = participantRepository.existByChannelPkAndMemberPkAndDeletedYN(userDetails.getMemberPk(), request.getChannelPk(), BaseTime.Yn.N);
        if(!exist) throw new ValidationException(ErrorCode.ACCESS_DENIED);

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

        boolean exist = participantRepository.existByChannelPkAndMemberPkAndDeletedYN(userDetails.getMemberPk(), channelPk, BaseTime.Yn.N);
        if(!exist) throw new ValidationException(ErrorCode.ACCESS_DENIED);

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
                    .boxNickname(ml.getBoxNickname())
                    .profileImageUrl(ml.getProfileImageUrl()).build();
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

    public void inviteMemberToAllPublicChannels(Long memberPk, Long boxPk){

        Member member = memberRepository.findById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        List<ChannelType> types = List.of(ChannelType.PUBLIC, ChannelType.NOTICE, ChannelType.GENERAL);

        List<Channel> channelList = channelRepository.findByBoxBoxPkAndDeletedYNAndChannelTypeIn(boxPk, BaseTime.Yn.N, types);

        List<ChannelParticipant> insertList = new ArrayList<>();

        List<ChannelSnapshot> snapshots = new ArrayList<>();

        for(Channel ch : channelList) {
            ChannelParticipant p = ChannelParticipant.builder()
                    .channel(ch)
                    .member(member)
                    .build();
            insertList.add(p);

            String boxCode = ch.getBox().getBoxCode(); // 안전: 같은 트랜잭션 내
            snapshots.add(new ChannelSnapshot(boxCode, ch.getChannelId(), ch.getChannelPk()));
        }

        participantRepository.saveAll(insertList);

        //DB 커밋 후 firestore 업데이트
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WriteBatch batch = firestore.batch();
                for(ChannelSnapshot ch : snapshots) {
                    DocumentReference docRef = firestore.collection("boxes")
                            .document(ch.boxCode)
                            .collection("channels")
                            .document(ch.channelId);
                    batch.update(docRef,"memberPks",com.google.cloud.firestore.FieldValue.arrayUnion(memberPk));
                }
                try {
                    ApiFuture<List<WriteResult>> commitFuture = batch.commit();
                    commitFuture.get();
                    log.info("Firestore updated for member {} in {} channels", memberPk, snapshots.size());
                } catch (Exception e) {
                    log.error("Failed to update Firestore memberPks for member {}: {}", memberPk, e, e);
                }
            }
        });
    }

    public void removeMemberFromAllPublicChannels(Long memberPk, Long boxPk){

        memberRepository.findById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        List<ChannelType> types = List.of(ChannelType.PUBLIC, ChannelType.NOTICE, ChannelType.GENERAL);

        List<Channel> channelList = channelRepository.findByBoxBoxPkAndDeletedYNAndChannelTypeIn(boxPk, BaseTime.Yn.N, types);

        List<ChannelSnapshot> snapshots = channelList.stream()
                .map(ch -> new ChannelSnapshot(ch.getBox().getBoxCode(), ch.getChannelId(), ch.getChannelPk()))
                .toList();

        participantRepository.markDeletedByMemberPkAndChannelIn(memberPk, channelList, BaseTime.Yn.Y);

        //DB 커밋 후 firestore 업데이트
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                WriteBatch batch = firestore.batch();
                for(ChannelSnapshot ch : snapshots) {
                    DocumentReference docRef = firestore.collection("boxes")
                            .document(ch.boxCode)
                            .collection("channels")
                            .document(ch.channelId);
                    batch.update(docRef,"memberPks",com.google.cloud.firestore.FieldValue.arrayRemove(memberPk));
                }
                try {
                    ApiFuture<List<WriteResult>> commitFuture = batch.commit();
                    commitFuture.get();
                    log.info("Firestore removed for member {} in {} channels", memberPk, snapshots.size());
                } catch (Exception e) {
                    log.error("Failed to remove Firestore memberPks for member {}: {}", memberPk, e, e);
                }
            }
        });
    }

    public MessageSendResponse sendMessage(Long channelPk, MessageSendRequest request, CustomUserDetails userDetails) {

        Channel channel = channelRepository.findById(channelPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.CHANNEL_NOT_FOUND));

        boolean isParticipant = participantRepository.existByChannelPkAndMemberPkAndDeletedYN(
                userDetails.getMemberPk(), channelPk, BaseTime.Yn.N);
        if (!isParticipant) throw new ValidationException(ErrorCode.FORBIDDEN);

        Long boxPk = channel.getBox().getBoxPk();
        String boxCode = channel.getBox().getBoxCode();
        String channelId = channel.getChannelId();

        MemberList ml = memberListRepository.findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(
                        userDetails.getMemberPk(), boxPk, BaseTime.Yn.N)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        String senderId = userDetails.getMemberPk().toString();
        String boxNickname = ml.getBoxNickname();
        Long memberListPk = ml.getMemberListPk();
        String profileImageUrl = ml.getProfileImageUrl();

        com.google.cloud.firestore.CollectionReference messagesRef = firestore
                .collection("boxes").document(boxCode)
                .collection("channels").document(channelId)
                .collection("messages");

        com.google.cloud.firestore.DocumentReference docRef = (request.getClientMessageId() != null && !request.getClientMessageId().isBlank())
                ? messagesRef.document(request.getClientMessageId())
                : messagesRef.document();

        String messageId = docRef.getId();
        long sentAtMillis = System.currentTimeMillis();
        com.google.cloud.Timestamp sentAt = com.google.cloud.Timestamp.ofTimeSecondsAndNanos(
                sentAtMillis / 1000, (int) ((sentAtMillis % 1000) * 1_000_000));

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("type", "user_message");
        messageData.put("schemaVersion", 1);
        messageData.put("senderType", "user");
        messageData.put("source", "server");
        messageData.put("senderId", senderId);
        messageData.put("text", request.getText().trim());
        messageData.put("sentAt", sentAt);
        messageData.put("memberListPk", memberListPk);
        if (request.getParentMessageId() != null) {
            messageData.put("parentMessageId", request.getParentMessageId());
        }
        if (boxNickname != null && !boxNickname.isBlank()) {
            messageData.put("boxNickname", boxNickname);
        }
        if (channel.getChannelName() != null) {
            messageData.put("channelName", channel.getChannelName());
        }
        if (profileImageUrl != null && !profileImageUrl.isBlank()) {
            messageData.put("senderProfileImageUrl", profileImageUrl);
        }

        try {
            docRef.set(messageData).get();
        } catch (Exception e) {
            throw new RuntimeException("메시지 Firestore 저장 실패", e);
        }

        // 채널 목록 미리보기용 lastMessage 업데이트
        Map<String, Object> lastMessage = new HashMap<>();
        lastMessage.put("text", request.getText().trim());
        lastMessage.put("senderId", senderId);
        lastMessage.put("sentAt", sentAt);
        lastMessage.put("type", "user_message");
        if (boxNickname != null && !boxNickname.isBlank()) {
            lastMessage.put("boxNickname", boxNickname);
        }

        try {
            firestore.collection("boxes").document(boxCode)
                    .collection("channels").document(channelId)
                    .set(Map.of("lastMessage", lastMessage), com.google.cloud.firestore.SetOptions.merge());
        } catch (Exception e) {
            log.warn("lastMessage 업데이트 실패 channelId={}: {}", channelId, e.getMessage());
        }

        // push dispatch 내부 트리거
        try {
            MessagePushRequest pushRequest = MessagePushRequest.builder()
                    .boxCode(boxCode)
                    .channelId(channelId)
                    .channelName(channel.getChannelName())
                    .messageId(messageId)
                    .sentAtMillis(sentAtMillis)
                    .senderId(userDetails.getMemberPk())
                    .boxNickname(boxNickname)
                    .memberListPk(memberListPk)
                    .text(request.getText().trim())
                    .type("user_message")
                    .build();
            alarmService.dispatch(pushRequest);
        } catch (Exception e) {
            log.error("push dispatch 실패 (메시지는 저장됨) messageId={}: {}", messageId, e.getMessage());
        }

        return MessageSendResponse.builder()
                .messageId(messageId)
                .sentAtMillis(sentAtMillis)
                .build();
    }

    private static class ChannelSnapshot {
        final String boxCode;
        final String channelId;
        final Long channelPk;
        ChannelSnapshot(String boxCode, String channelId, Long channelPk) {
            this.boxCode = boxCode;
            this.channelId = channelId;
            this.channelPk = channelPk;
        }
    }
}
