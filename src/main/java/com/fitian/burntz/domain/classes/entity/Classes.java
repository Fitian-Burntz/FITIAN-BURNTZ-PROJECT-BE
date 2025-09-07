package com.fitian.burntz.domain.classes.entity;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.classes.entity
 * @fileName : Classes
 * @date : 2025-09-04
 * @description : Classes 엔티티
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "classes")
public class Classes extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_pk")
    private Long classPk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_pk", nullable = false)
    private Box box;

    @Column(name = "class_date")
    private LocalDate classDate;

    @Column(name = "start_time", length = 50)
    private String startTime;

    @Column(name = "end_time", length = 50)
    private String endTime;

    @Column(name = "class_member_capacity")
    private Integer classMemberCapacity;

    @Column(name = "class_title", length = 100)
    private String classTitle;

    @Column(name = "class_memo", length = 255)
    private String classMemo;
}