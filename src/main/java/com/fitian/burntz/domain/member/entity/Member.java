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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private String deletedAt;


    /** 멤버 계정 생성 정적 메서드 **/
    public static Member create(
            String memberId, String nickname, String email, Gender gender){

        LocalDateTime now = LocalDateTime.now();

        return Member.builder()
                .memberId(memberId)
                .nickname(nickname)
                .email(email)
                .gender(gender)
                .createdAt(now)
                .deletedAt("N")
                .build();
    }

}
