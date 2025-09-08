package com.fitian.burntz.domain.auth.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;


@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "auth",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_pk", "device_id"})
)
public class Auth extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_pk")
    private Long authPk;

    @Column(name = "device_id", length = 100, nullable = false)
    private String deviceId;

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    // Member 엔티티가 있으면 연관관계로 매핑 (지연 로딩)
    // 없으면 Long memberPk 로 대체 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonBackReference
    @JoinColumn(name = "member_pk")
    private Member member; // 실제 Member 엔티티 타입으로 변경하세요

    /**
     * 리프레시 토큰 갱신 — updatedAt만 즉시 갱신
     */
    public void updateRefreshToken(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
        setUpdatedAtToNow();          // BaseTime에 추가한 헬퍼 사용
    }

    /**
     * deviceId 갱신 (도메인 행위)
     */
    public void updateDeviceId(String deviceId) {
        this.deviceId = deviceId;
        setUpdatedAtToNow();
    }

    /**
     * 정적 팩토리 메서드: Member 연관관계로 새로운 Auth 생성
     * (도메인 규칙이 생기면 여기서 검증/초기화 처리)
     */
    public static Auth createForMember(Member member, String hashedRefreshToken, String deviceId) {
        return Auth.builder()
                .member(member)
                .refreshToken(hashedRefreshToken)
                .deviceId(deviceId)
                .build();
    }

    /**
     * 로그아웃 등에서 리프레시 토큰을 제거할 때 사용
     */
    public void clearRefreshToken() {
        this.refreshToken = null;
        setUpdatedAtToNow();
    }


}
