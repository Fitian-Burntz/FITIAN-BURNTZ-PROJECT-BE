package com.fitian.burntz.domain.channel.entity;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.channel.entity
 * @fileName : ChannelParticipant
 * @date : 2025-09-04
 * @description : 채널 참여자 entity
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "channel_participant")
public class ChannelParticipant extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_participant_pk")
    private Long channelParticipantPk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_pk", nullable = false)
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_pk", nullable = false)
    private Member member;
}