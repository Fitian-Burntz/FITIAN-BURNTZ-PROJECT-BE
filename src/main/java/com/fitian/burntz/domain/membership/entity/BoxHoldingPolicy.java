package com.fitian.burntz.domain.membership.entity;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "box_holding_policy")
public class BoxHoldingPolicy extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_pk")
    private Long policyPk;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_pk", nullable = false, unique = true)
    private Box box;

    @Column(name = "default_hold_days")
    private Integer defaultHoldDays;

    public void update(Integer defaultHoldDays) {
        this.defaultHoldDays = defaultHoldDays;
    }
}
