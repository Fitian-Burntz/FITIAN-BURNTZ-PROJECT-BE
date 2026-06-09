package com.fitian.burntz.infra.payment.v1.controller;

import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.fitian.burntz.infra.payment.docs.PaymentDocs;
import com.fitian.burntz.infra.payment.dto.response.PurchaseLogResponse;
import com.fitian.burntz.infra.payment.service.PaymentService;
import com.fitian.burntz.infra.payment.v1.dto.PaymentSyncRequest;
import com.fitian.burntz.infra.payment.v1.dto.PaymentSyncResponse;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * @author : 홍준표
 * @packageName : com.fitian.burntz.infra.payment.v1.controller
 * @fileName : PaymentController
 * @date : 2025-09-09
 * @description : revenuecat 결제 요청 이후 웹훅으로 결제 완료 데이터를 받는 컨트롤러
 */

@RestController
@Slf4j
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController implements PaymentDocs {

  private final PaymentService paymentService;

  @Override
  @PostMapping("/webhook/purchase")
  public ResponseEntity<?> handlePuchaseWebhook(@RequestBody WebhookPurchaseResponse webhookPurchaseResponse,
      HttpServletRequest request) {
    paymentService.handlePuchaseWebhook(webhookPurchaseResponse, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/webhook/cancel")
  public ResponseEntity<?> handleCancelWebhook(
          @RequestBody WebhookPurchaseResponse webhookPurchaseResponse,
          HttpServletRequest request
  ) {
    paymentService.handleCancelWebhook(webhookPurchaseResponse, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/webhook/expiration")
  public ResponseEntity<?> handleExpirationWebhook(
          @RequestBody WebhookPurchaseResponse webhookPurchaseResponse,
          HttpServletRequest request
  ) {
    paymentService.handleExpirationWebhook(webhookPurchaseResponse, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/webhook/uncancel")
  public ResponseEntity<?> handleUncancelWebhook(
          @RequestBody WebhookPurchaseResponse webhookPurchaseResponse,
          HttpServletRequest request
  ) {
    paymentService.handleUncancelWebhook(webhookPurchaseResponse, request);
    return ResponseEntity.ok().build();
  }

//  @Override
//  @PostMapping("/purchase/refund/{boxPk}")


  @PostMapping("/sync")
  public ApiResponse<PaymentSyncResponse> syncPayment(
          @AuthenticationPrincipal CustomUserDetails userDetails,
          @Valid @RequestBody PaymentSyncRequest request) {

    PaymentSyncResponse response =
            paymentService.syncPayment(userDetails.getMemberPk(), request.getBoxPk());

    return ApiResponse.success(response);
  }


  @Override
  @GetMapping("/purchase/log/{boxPk}")
  public ApiResponse<List<PurchaseLogResponse>> getPurchaseLog(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable(value = "boxPk") Long boxPk) {
    List<PurchaseLogResponse> response = paymentService.getPurchaseLog(userDetails.getMemberPk(), boxPk);
    return ApiResponse.success(response);
  }

}
