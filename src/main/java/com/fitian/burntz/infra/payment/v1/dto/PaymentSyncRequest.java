package com.fitian.burntz.infra.payment.v1.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.infra.payment.v1.dto
 * @fileName : PaymentSyncRequest
 * @date : 2026-03-15
 * @description :
 */

@Getter
@Setter
@NoArgsConstructor
public class PaymentSyncRequest {
    @NotNull
    private Long boxPk;
}
