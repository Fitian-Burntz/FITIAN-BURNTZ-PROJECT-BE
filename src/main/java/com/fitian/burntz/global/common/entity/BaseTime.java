package com.fitian.burntz.global.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.global.common.enums
 * @fileName : BaseTime
 * @date : 2025-09-04
 * @description : 모든 테이블에서 사용하는 공통 속성 정의
 */
@Getter
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
public abstract class BaseTime {

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_yn", length = 1, nullable = false)
    @Enumerated(EnumType.STRING)
    private Yn deletedYN = Yn.N;

    //soft delete용 enum
    public enum Yn {
        Y, N
    }
}