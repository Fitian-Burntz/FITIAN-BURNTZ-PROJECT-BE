package com.fitian.burntz.domain.auth.entity;

import com.fitian.burntz.domain.member.entity.Member;
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
public class Auth {

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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_yn", length = 1)
    private String deletedYn; // "Y" / "N" 으로 사용


    /**
     * 리프레시 토큰만 갱신하는 편의 메서드 (mutable)
     */
    public void updateRefreshToken(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
        this.updatedAt = LocalDateTime.now();
    }


    /**
     * 삭제 처리: deletedYn 을 "Y"로 변경하고 updatedAt을 현재 시각으로 갱신.
     */
    public void markDeleted() {
        if (!"Y".equals(this.deletedYn)) {
            this.deletedYn = "Y";
            this.updatedAt = LocalDateTime.now();
        }
    }


}
