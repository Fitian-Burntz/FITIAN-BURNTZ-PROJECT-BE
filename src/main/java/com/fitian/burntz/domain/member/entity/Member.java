package com.fitian.burntz.domain.member.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fitian.burntz.domain.auth.entity.Auth;
import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider","member_id"}))
public class Member extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_pk")
    private Long memberPk;

    @Column(name = "member_id", length = 50, nullable = false)
    private String memberId;

    @Column(name = "nickname", length = 50, nullable = false)
    private String nickname;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "gender", length = 10, nullable = false)
    private Gender gender = Gender.OTHERS;

    // provider 추가 (google, apple 등)
    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    // Member -> Auth 연관관계 추가 (초기화해서 NPE 방지)
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @JsonManagedReference(value = "member-auth")
    @Builder.Default
    @ToString.Exclude
    private List<Auth> auths = new ArrayList<>();

    // 멤버 -> 멤버 리스트 로 조회를 해야함
    @OneToMany(mappedBy = "member")
    @JsonManagedReference
    @Builder.Default
    @ToString.Exclude
    private List<MemberList> memberLists = new ArrayList<>();


    // nullable 허용
    @Column(name = "last_visited_box_pk")
    private Long lastVisitedBoxPk;


    /** 멤버 계정 생성 정적 메서드 **/
    public static Member create(
            String memberId, String nickname, String email, String provider){

        return Member.builder()
                .memberId(memberId)
                .nickname(nickname)
                .email(email)
                .gender(Gender.OTHERS)
                .provider(provider)
                .build();
    }

    /** Member 정보 업데이트 **/
    public boolean updateMemberProfile(String nickname, String email, Gender gender) {

        boolean changed = false;

        if (nickname != null && !nickname.isBlank() && !nickname.equals(this.nickname)) {
            this.nickname = nickname;
            changed = true;
        }
        if (email != null && !email.isBlank() && !email.equals(this.email)) {
            this.email = email;
            changed = true;
        }
        if (gender != null && gender != this.gender) {
            this.gender = gender;
            changed = true;
        }
        return changed;
    }

}
