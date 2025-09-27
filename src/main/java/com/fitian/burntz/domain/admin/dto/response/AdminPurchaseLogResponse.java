package com.fitian.burntz.domain.admin.dto.response;

import com.fitian.burntz.domain.box.enums.SubscriptionStatus;
import com.fitian.burntz.infra.payment.dto.response.PurchaseLogResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AdminPurchaseLogResponse {

  private LocalDateTime startedAt;
  private LocalDateTime expiredAt;
  private LocalDateTime currentAt;
  private SubscriptionStatus status;

  private List<PurchaseLogResponse> logs;


}
