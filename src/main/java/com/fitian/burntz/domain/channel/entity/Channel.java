package com.fitian.burntz.domain.channel.entity;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.channel.enums.ChannelType;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.channel.entity
 * @fileName : Channel
 * @date : 2025-09-04
 * @description : Channel 엔티티
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "channel")
public class Channel extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "channel_pk")
    private Long channelPk;

    @Column(name = "channel_id", length = 255)
    private String channelId;

    @Column(name = "channel_name", length = 100)
    private String channelName;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 100)
    private ChannelType channelType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_pk", nullable = false)
    private Box box;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;
}