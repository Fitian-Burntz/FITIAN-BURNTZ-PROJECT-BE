package com.fitian.burntz.domain.box.entity;

import com.fitian.burntz.domain.box.enums.ActivityType;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "box_activity",
        indexes = {
                @Index(name = "idx_box_activity_box_pk", columnList = "box_pk"),
                @Index(name = "idx_box_activity_created_at", columnList = "created_at")
        })
public class BoxActivity extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_pk")
    private Long activityPk;

    @Column(name = "box_pk", nullable = false)
    private Long boxPk;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50, nullable = false)
    private ActivityType type;

    @Column(name = "actor_pk", nullable = false)
    private Long actorPk;

    @Column(name = "actor_name", length = 100, nullable = false)
    private String actorName;

    @Column(name = "target_member_pk")
    private Long targetMemberPk;

    @Column(name = "target_member_name", length = 100)
    private String targetMemberName;

    @Column(name = "detail", length = 255)
    private String detail;
}
