package com.fitian.burntz.domain.auth.entity;

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
@Table(name = "auth")
public class Auth extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_pk")
    private Long authPk;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    // Member 엔티티가 있으면 연관관계로 매핑 (지연 로딩)
    // 없으면 Long memberPk 로 대체 가능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_pk")
    private Member member; // 실제 Member 엔티티 타입으로 변경하세요


    /**
     * 리프레시 토큰 갱신 — updatedAt만 즉시 갱신
     */
    public void updateRefreshToken(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
        setUpdatedAtToNow();          // BaseTime에 추가한 헬퍼 사용
    }


}
