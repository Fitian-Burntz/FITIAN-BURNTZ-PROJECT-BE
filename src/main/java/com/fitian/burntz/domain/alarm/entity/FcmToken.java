package com.fitian.burntz.domain.alarm.entity;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.alarm.entity
 * @fileName : FcmToken
 * @date : 2025-09-04
 * @description : FcmToken 엔티티
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fcm_token")
public class FcmToken extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_pk")
    private Long tokenPk;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "token",columnDefinition="text")
    private String token;

    @Column(name = "is_active", length = 1)
    private String isActive;    //0 or 1

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_pk", nullable = false)
    private Member member;
}