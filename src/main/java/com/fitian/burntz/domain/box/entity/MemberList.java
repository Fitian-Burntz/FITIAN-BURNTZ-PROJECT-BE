package com.fitian.burntz.domain.box.entity;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.box.entity
 * @fileName : MemberList
 * @date : 2025-09-04
 * @description : 멤버리스트 entity
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member_list")
public class MemberList extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_list_pk")
    private Long memberListPk;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 15)
    private MemberRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_pk", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_pk", nullable = false)
    private Box box;
}