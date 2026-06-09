package com.fitian.burntz.domain.admin.payment.controller;

import com.fitian.burntz.domain.admin.dto.AdminAccount;
import com.fitian.burntz.domain.admin.dto.response.AdminBoxesResponse;
import com.fitian.burntz.domain.admin.dto.response.AdminPurchaseLogResponse;
import com.fitian.burntz.domain.admin.payment.service.AdminPaymentService;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.entity.BoxSubscription;
import com.fitian.burntz.domain.box.entity.SubscriptionEventLog;
import com.fitian.burntz.domain.box.enums.SubscriptionStatus;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.box.repository.BoxSubscriptionRepository;
import com.fitian.burntz.domain.box.repository.SubscriptionEventLogRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.NotFoundException;
import com.fitian.burntz.infra.payment.dto.response.PurchaseLogResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminPaymentController {

  private final BoxRepository boxRepository;
  private final BoxSubscriptionRepository boxSubscriptionRepository;
  private final AdminPaymentService adminPaymentService;
  private final AdminAccount adminAccount;
  private final SubscriptionEventLogRepository subscriptionEventLogRepository;

  @GetMapping("/boxes")
  public ApiResponse<List<AdminBoxesResponse>> getBoxes(HttpServletRequest request) {
     if(adminAccount.validateAccount(request)) {
       List<Box> boxes = boxRepository.findAllOrderByBoxPkAsc();
       List<AdminBoxesResponse> adminBoxesResponses = boxes.stream()
           .map(box -> AdminBoxesResponse
               .builder()
               .boxPk(box.getBoxPk())
               .boxName(box.getBoxName())
               .build()).toList();

       return ApiResponse.success(adminBoxesResponses);
     }

     log.info("[Admin] 관리자 인증에 실패하여 박스 목록을 불러올 수 없습니다.");
     return ApiResponse.success(List.of());
  }

  @GetMapping("/purchase/log/{boxPk}")
  public ApiResponse<AdminPurchaseLogResponse> getPurchaseLog(
      @PathVariable(value = "boxPk") Long boxPk,
      HttpServletRequest request) {

    if(adminAccount.validateAccount(request)) {

      BoxSubscription boxSubscription = boxSubscriptionRepository.findByBoxPk(boxPk).orElseThrow(
          () -> new NotFoundException(ErrorCode.BOX_NOT_FOUND)
      );

      List<PurchaseLogResponse> purchaseLogResponses = adminPaymentService.getPurchaseLog(boxPk);

      AdminPurchaseLogResponse adminPurchaseLogResponse = AdminPurchaseLogResponse
          .builder()
          .startedAt(boxSubscription.getStartedAt())
          .currentAt(LocalDateTime.now())
          .expiredAt(boxSubscription.getExpiredAt())
          .status(boxSubscription.getStatus())
          .logs(purchaseLogResponses)
          .build();


      return ApiResponse.success(adminPurchaseLogResponse);
    }
    return ApiResponse.success(null);
  }



  @PutMapping("/purchase/status/{boxPk}/{status}")
  @Transactional
  public void updatePurchaseStatus(
      @PathVariable(value = "boxPk") Long boxPk,
      @PathVariable(value = "status") String status,
      HttpServletRequest request) {

    if(adminAccount.validateAccount(request)) {

      BoxSubscription boxSubscription = boxSubscriptionRepository.findByBoxPk(boxPk).orElseThrow(
          () -> new NotFoundException(ErrorCode.BOX_NOT_FOUND)
      );
      Member member = boxSubscription.getMember();
      Box box = boxSubscription.getBox();


        switch (status) {
            case "활성" -> {
                boxSubscription.updateStatus(SubscriptionStatus.ACTIVE);
                box.subscribe();
                SubscriptionEventLog subscriptionEventLog = SubscriptionEventLog.of(
                        member,
                        box,
                        boxSubscription.getProductId() + "(관리자에 의해 활성화됨)",
                        boxSubscription.getStore(),
                        SubscriptionStatus.ACTIVE,
                        null,
                        null,
                        0.0,
                        null,
                        boxSubscription.getMember().getMemberId(),
                        null,
                        null
                );
                subscriptionEventLogRepository.save(subscriptionEventLog);
            }
            case "만료" -> {
                boxSubscription.updateStatus(SubscriptionStatus.EXPIRED);
                box.unsubscribe();
                SubscriptionEventLog subscriptionEventLog = SubscriptionEventLog.of(
                        member,
                        box,
                        boxSubscription.getProductId() + "(관리자에 의해 만료 처리됨)",
                        boxSubscription.getStore(),
                        SubscriptionStatus.EXPIRED,
                        null,
                        null,
                        0.0,
                        null,
                        boxSubscription.getMember().getMemberId(),
                        null,
                        null
                );
                subscriptionEventLogRepository.save(subscriptionEventLog);
            }
            case "취소" -> {
                boxSubscription.updateStatus(SubscriptionStatus.CANCELED);
                box.unsubscribe();
                SubscriptionEventLog subscriptionEventLog = SubscriptionEventLog.of(
                        member,
                        box,
                        boxSubscription.getProductId() + "(관리자에 의해 취소 처리됨)",
                        boxSubscription.getStore(),
                        SubscriptionStatus.CANCELED,
                        null,
                        null,
                        0.0,
                        null,
                        boxSubscription.getMember().getMemberId(),
                        null,
                        null
                );
                subscriptionEventLogRepository.save(subscriptionEventLog);
            }
            case "환불" -> {
                boxSubscription.updateStatus(SubscriptionStatus.REFUNDED);
                box.unsubscribe();
                SubscriptionEventLog subscriptionEventLog = SubscriptionEventLog.of(
                        member,
                        box,
                        boxSubscription.getProductId() + "(관리자에 의해 환불 처리됨)",
                        boxSubscription.getStore(),
                        SubscriptionStatus.REFUNDED,
                        null,
                        null,
                        0.0,
                        null,
                        boxSubscription.getMember().getMemberId(),
                        null,
                        null
                );
                subscriptionEventLogRepository.save(subscriptionEventLog);

            }
            case "보류" -> {
                boxSubscription.updateStatus(SubscriptionStatus.PENDING);
                box.unsubscribe();
                SubscriptionEventLog subscriptionEventLog = SubscriptionEventLog.of(
                        member,
                        box,
                        boxSubscription.getProductId() + "(관리자에 의해 보류 처리됨)",
                        boxSubscription.getStore(),
                        SubscriptionStatus.PENDING,
                        null,
                        null,
                        0.0,
                        null,
                        boxSubscription.getMember().getMemberId(),
                        null,
                        null
                );
                subscriptionEventLogRepository.save(subscriptionEventLog);
            }
        }
    }
  }

}
