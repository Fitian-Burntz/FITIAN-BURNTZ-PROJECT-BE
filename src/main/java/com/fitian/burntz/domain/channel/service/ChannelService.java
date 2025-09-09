package com.fitian.burntz.domain.channel.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.channel.v1.dto.ChannelCreateRequest;
import com.fitian.burntz.domain.channel.entity.Channel;
import com.fitian.burntz.domain.channel.repository.ChannelRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.service
 * @fileName : ChannelService
 * @date : 2025-09-08
 * @description : 채널(채팅) 서비스 입니다.
 */

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final BoxRepository boxRepository;
    private final MemberRepository memberRepository;

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

        channelRepository.save(channel);
    }
}
