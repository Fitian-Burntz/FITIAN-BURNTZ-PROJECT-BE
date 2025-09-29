package com.fitian.burntz.domain.admin.payment.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.entity.SubscriptionEventLog;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.box.repository.SubscriptionEventLogRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.NotFoundException;
import com.fitian.burntz.infra.payment.dto.response.PurchaseLogResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminPaymentService {

  private final BoxRepository boxRepository;
  private final SubscriptionEventLogRepository subscriptionEventLogRepository;

  public List<PurchaseLogResponse> getPurchaseLog(Long bokPk) {

    Box box = boxRepository.findById(bokPk)
        .orElseThrow(() -> new NotFoundException(ErrorCode.BOX_NOT_FOUND));

    List<SubscriptionEventLog> subscriptionEventLogs = subscriptionEventLogRepository.findAllByBoxPk(bokPk);

    if(subscriptionEventLogs.isEmpty()) {
      return List.of();
    }

    List<PurchaseLogResponse> responses = subscriptionEventLogs
        .stream()
        .map(log -> {
          PurchaseLogResponse purchaseLogResponse = PurchaseLogResponse
              .builder()
              .purchasedAt(log.getCreatedAt())
              .productId(log.getProductId())
              .price(log.getPrice())
              .build();
          return purchaseLogResponse;
        }).toList();

    return responses;
  }

}
