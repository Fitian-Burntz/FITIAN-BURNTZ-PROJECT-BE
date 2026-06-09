package com.fitian.burntz.infra.payment.docs;

import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.fitian.burntz.infra.payment.dto.response.PurchaseLogResponse;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "구매 관련 api 입니다.", description = "구매 이력을 조회하거나 웹훅을 통해 결제 완료 데이터를 받습니다.")
public interface PaymentDocs {

  @Operation(summary = "결제 완료 웹훅 엔드포인트", description = "해당 웹훅을 통해 결제 완료 데이터를 받습니다.")
  public ResponseEntity<?> handlePuchaseWebhook(WebhookPurchaseResponse webhookPurchaseResponse, HttpServletRequest request);

  @Operation(summary = "구매 이력 조회", description = "boxPk로 해당 박스의 구매 이력을 조회합니다. 박스 OWNER만 조회 가능합니다.")
  public ApiResponse<List<PurchaseLogResponse>> getPurchaseLog(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      Long boxPk);
}

