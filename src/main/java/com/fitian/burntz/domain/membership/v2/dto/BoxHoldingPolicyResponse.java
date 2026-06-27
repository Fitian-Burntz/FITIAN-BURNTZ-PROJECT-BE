package com.fitian.burntz.domain.membership.v2.dto;

import com.fitian.burntz.domain.membership.entity.BoxHoldingPolicy;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoxHoldingPolicyResponse {

    private Long boxPk;
    private Integer defaultHoldDays;

    public static BoxHoldingPolicyResponse from(BoxHoldingPolicy policy) {
        return BoxHoldingPolicyResponse.builder()
                .boxPk(policy.getBox().getBoxPk())
                .defaultHoldDays(policy.getDefaultHoldDays())
                .build();
    }
}
