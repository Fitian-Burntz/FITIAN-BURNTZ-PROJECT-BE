package com.fitian.burntz.domain.member.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fitian.burntz.domain.auth.entity.Auth;
import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import jdk.jfr.Enabled;
import lombok.*;

import java.time.LocalDateTime;
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
    @Column(name = "gender", length = 10, nullable = false)
    private Gender gender = Gender.OTHERS;

    // provider 추가 (google, apple 등)
    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    // Member -> Auth 연관관계 추가 (초기화해서 NPE 방지)
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Auth> auths = new ArrayList<>();

    // 멤버 -> 멤버 리스트 로 조회를 해야함
    @OneToMany(mappedBy = "member")
    @JsonManagedReference
    private List<MemberList> memberLists = new ArrayList<>();


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

    // Member 엔티티 내부에 추가
    public void updateProfileIfChanged(String nickname, String email, Gender gender) {
        if (nickname != null && !nickname.isBlank() && !nickname.equals(this.nickname)) {
            this.nickname = nickname;
        }
        if (email != null && !email.isBlank() && !email.equals(this.email)) {
            this.email = email;
        }
        if (gender != null && gender != this.gender) {
            this.gender = gender;
        }
    }


    /**
     * Soft delete 처리 — 부모에 구현한 로직 재사용
     * 그리고 연관된 Auth들도 soft-delete 처리
     */
    public void markDeleted() {
        super.markDeleted();          // BaseTime.markDeleted()
        // auths가 LAZY면 트랜잭션 내에서 접근해야 로드된다.
        if (this.auths != null) {
            this.auths.forEach(auth -> {
                if (!auth.isDeleted()) {   // Auth에 isDeleted() 헬퍼가 있으면 더 안전
                    auth.markDeleted();
                }
            });
        }
    }



}
