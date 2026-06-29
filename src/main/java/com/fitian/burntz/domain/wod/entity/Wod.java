package com.fitian.burntz.domain.wod.entity;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.wod.enums.WodType;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.wod.entity
 * @fileName : Wod
 * @date : 2025-09-04
 * @description : WodEntity
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "wod")
public class Wod extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wod_pk")
    private Long wodPk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_pk", nullable = false)
    private Box box;

    @Column(name = "wod_title", length = 100)
    private String wodTitle;

    @Column(name = "wod_script",columnDefinition="text")
    private String wodScript;

    @Enumerated(EnumType.STRING)
    @Column(name = "wod_type", length = 100)
    private WodType wodType;

    @Column(name = "wod_date")
    private LocalDate wodDate;

    // Wod update — null이면 기존값 유지, 빈 문자열("")이면 null로 초기화(타이틀 제거)
    public void update(String wodTitle, String wodScript, WodType wodType) {
        if (wodTitle != null) this.wodTitle = wodTitle.isBlank() ? null : wodTitle;
        if (wodScript != null && !wodScript.isBlank()) this.wodScript = wodScript;
        if (wodType != null) this.wodType = wodType;
        setUpdatedAtToNow();
    }
}