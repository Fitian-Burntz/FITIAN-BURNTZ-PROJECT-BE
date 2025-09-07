package com.fitian.burntz.domain.member.entity;

import com.fitian.burntz.domain.member.member_enum.Gender;
import jakarta.persistence.*;
import jdk.jfr.Enabled;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider","member_id"}))
public class Member {

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

    @Column(name = "gender", length = 10, nullable = false)
    private Gender gender;

    // provider 추가 (google, apple 등)
    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private String deletedAt;


    /** 멤버 계정 생성 정적 메서드 **/
    public static Member create(
            String memberId, String nickname, String email, Gender gender, String provider){

        LocalDateTime now = LocalDateTime.now();

        return Member.builder()
                .memberId(memberId)
                .nickname(nickname)
                .email(email)
                .gender(gender)
                .provider(provider)
                .createdAt(now)
                .deletedAt("N")
                .build();
    }

}
