package com.fitian.burntz.domain.member.entity;

import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.global.common.entity.BaseTime;
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

    @Column(name = "gender", length = 10, nullable = false)
    private Gender gender;

    // provider 추가 (google, apple 등)
    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    /** 멤버 계정 생성 정적 메서드 **/
    public static Member create(
            String memberId, String nickname, String email, Gender gender, String provider){

        return Member.builder()
                .memberId(memberId)
                .nickname(nickname)
                .email(email)
                .gender(gender)
                .provider(provider)
                .build();
    }

}
