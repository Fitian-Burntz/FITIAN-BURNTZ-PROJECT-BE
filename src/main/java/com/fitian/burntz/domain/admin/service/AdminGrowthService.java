package com.fitian.burntz.domain.admin.service;

import com.fitian.burntz.domain.admin.dto.response.AdminGrowthResponse;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminGrowthService {

    private final BoxRepository boxRepository;
    private final MemberListRepository memberListRepository;

    public List<AdminGrowthResponse.NewBoxInfo> getRecentBoxes(int limit) {
        int safeLimit = Math.min(limit, 100);
        return boxRepository.findRecentActiveBoxes(PageRequest.of(0, safeLimit)).stream()
                .map(b -> AdminGrowthResponse.NewBoxInfo.builder()
                        .boxPk(b.getBoxPk())
                        .boxName(b.getBoxName())
                        .boxCode(b.getBoxCode())
                        .boxAddress(b.getBoxAddress())
                        .subscribeStatus(b.getSubscribe().name())
                        .createdAt(b.getCreatedAt())
                        .build())
                .toList();
    }

    public List<AdminGrowthResponse.NewMemberJoinInfo> getRecentMemberJoins(int limit) {
        int safeLimit = Math.min(limit, 100);
        return memberListRepository.findRecentMemberJoins(PageRequest.of(0, safeLimit)).stream()
                .map(ml -> AdminGrowthResponse.NewMemberJoinInfo.builder()
                        .memberListPk(ml.getMemberListPk())
                        .boxPk(ml.getBox().getBoxPk())
                        .boxName(ml.getBox().getBoxName())
                        .memberPk(ml.getMember().getMemberPk())
                        .nickname(ml.getMember().getNickname())
                        .email(ml.getMember().getEmail())
                        .role(ml.getRole().name())
                        .joinedAt(ml.getCreatedAt())
                        .build())
                .toList();
    }
}
