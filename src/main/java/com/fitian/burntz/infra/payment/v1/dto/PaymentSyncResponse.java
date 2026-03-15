package com.fitian.burntz.infra.payment.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.infra.payment.v1.dto
 * @fileName : PaymentSyncResponse
 * @date : 2026-03-15
 * @description : 결제 싱크 응답 dto
 */

@Getter
@Builder
@AllArgsConstructor
public class PaymentSyncResponse {

    private Long boxPk;
    private boolean premium;
    private String productId;
    private String store;
    private LocalDateTime startedAt;
    private LocalDateTime expiredAt;
    private String syncedFrom;

}
