package com.fitian.burntz.infra.payment.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PurchaseLogResponse {

  private LocalDateTime purchasedAt; // = 구매일자 = createdAt(로그 db 필드명)
  private String productId; // revenuecat 상품 아이디
  private Double price; // 결제 금액
}
