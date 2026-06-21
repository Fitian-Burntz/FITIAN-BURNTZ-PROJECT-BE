package com.fitian.burntz.domain.locker.entity;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "locker")
public class Locker extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "locker_pk")
    private Long lockerPk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_pk", nullable = false)
    private Box box;

    @Column(name = "locker_number", length = 50, nullable = false)
    private String lockerNumber;
}
