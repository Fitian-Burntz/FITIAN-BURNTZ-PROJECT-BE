package com.fitian.burntz.domain.member.entity;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

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

    @Column(name = "box_nickname", length = 50, nullable = false)
    private String boxNickname;

    // 멤버 role 변경
    public void changeRole(MemberRole newRole) {
        Objects.requireNonNull(newRole, "newRole required");

        // 이미 같은 role 이면 아무 작업도 하지 않음 -> 불필요한 dirty 체크/DB UPDATE 방지
        if (Objects.equals(this.role, newRole)) return;

        this.role = newRole;

        // BaseTime 에 updatedAt 현재로 바꾸는 메서드 호출
        this.setUpdatedAtToNow();
    }

    // 멤버 멤버 리스트 생성
    public static MemberList create(Box box, Member member) {
        Objects.requireNonNull(member, "member required");
        Objects.requireNonNull(box, "box required");

        return MemberList.builder()
                .box(box)
                .member(member)
                .boxNickname(member.getNickname())
                .role(MemberRole.OWNER)
                .build();
    }

    public static MemberList joinNewMemberToBox(Member joinMember, Box belongBox) {
        Objects.requireNonNull(joinMember, "member required");
        Objects.requireNonNull(belongBox, "box required");

        return MemberList.builder()
                .box(belongBox)
                .member(joinMember)
                .boxNickname(joinMember.getNickname())
                .role(MemberRole.GUEST)
                .build();

    }
}