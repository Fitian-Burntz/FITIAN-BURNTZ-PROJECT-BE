package com.fitian.burntz.domain.classes.entity;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.classes.v1.dto.ClassesUpdateRequest;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
    @Column(name = "classes_pk")
    private Long classesPk;

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

    public void updateFrom(ClassesUpdateRequest req) {
        if (req == null) return;

        if (req.getClassTitle() != null && !req.getClassTitle().isBlank()) {
            this.classTitle = req.getClassTitle();
        }

        if (req.getClassMemo() != null && !req.getClassMemo().isBlank()) {
            this.classMemo = req.getClassMemo();
        }

        if (req.getClassMemberCapacity() != null) {
            this.classMemberCapacity = req.getClassMemberCapacity();
        }

        if (req.getStartTime() != null && !req.getStartTime().isBlank()) {
            this.startTime = req.getStartTime();
        }

        if (req.getEndTime() != null && !req.getEndTime().isBlank()) {
            this.endTime = req.getEndTime();
        }

        // classDate는 DTO에 포함되어 있지 않으므로 처리하지 않음.
        // 만약 classDate를 변경해야 한다면 DTO와 함께 처리 로직 추가.
    }

    /**
     * 개별 변경 메서드들 — 필요에 따라 서비스에서 사용
     */
    public void changeTitle(String title) {
        if (title == null || title.isBlank()) return;
        this.classTitle = title;
    }

    public void changeMemo(String memo) {
        if (memo == null) return;
        this.classMemo = memo;
    }

    public void changeCapacity(Integer capacity) {
        if (capacity == null) return;
        this.classMemberCapacity = capacity;
    }

    public void changeTimes(String startTime, String endTime) {
        if (startTime != null && !startTime.isBlank()) this.startTime = startTime;
        if (endTime != null && !endTime.isBlank()) this.endTime = endTime;
    }

    /**
     * 완전히 교체하는 유틸(주의: null 허용 시 해당 필드가 null로 덮어쓰여집니다)
     */
    public void replaceAll(String title, String memo, Integer capacity, String startTime, String endTime) {
        this.classTitle = title;
        this.classMemo = memo;
        this.classMemberCapacity = capacity;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}