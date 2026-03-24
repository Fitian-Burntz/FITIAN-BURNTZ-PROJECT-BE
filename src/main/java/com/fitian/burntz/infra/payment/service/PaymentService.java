package com.fitian.burntz.infra.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.entity.BoxSubscription;
import com.fitian.burntz.domain.box.entity.SubscriptionEventLog;
import com.fitian.burntz.domain.box.enums.SubscriptionStatus;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.box.repository.BoxSubscriptionRepository;
import com.fitian.burntz.domain.box.repository.SubscriptionEventLogRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.NotFoundException;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.infra.payment.client.RevenueCatClient;
import com.fitian.burntz.infra.payment.dto.response.PurchaseLogResponse;
import com.fitian.burntz.infra.payment.enums.PaymentEventType;
import com.fitian.burntz.infra.payment.enums.PaymentStore;
import com.fitian.burntz.infra.payment.v1.dto.PaymentSyncResponse;
import com.fitian.burntz.infra.payment.v1.dto.RevenueCatSubscriberResponse;
import com.fitian.burntz.infra.payment.v1.dto.WebhookPurchaseResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

  private final BoxSubscriptionRepository boxSubscriptionRepository;
  private final BoxRepository boxRepository;
  private final SubscriptionEventLogRepository subscriptionEventLogRepository;
  private final MemberRepository memberRepository;
  private final RevenueCatClient revenueCatClient;

  @Value("${revenuecat.secret.key}")
  private String revenueCatWebhookAuthorization;

  private final ObjectMapper objectMapper;

  /**
   * [결제 완료 웹훅 처리 메서드]
   * @param webhookPurchaseResponse 결제 완료 웹훅 데이터
   * 결제완료 처리 흐름..
   * 1. 토큰 검증
   * 2. 멤버가 존재하는지 확인
   * 3. 박스가 존재하는지 확인
   * 4. box_subscription 테이블 업데이트 또는 삽입
   * 5. 구독 정보 저장
   * 6. 구독 로그 저장
   * 7. 최종 BOX 구독상태 변경
   */
  @Transactional
  public void handlePuchaseWebhook(WebhookPurchaseResponse webhookPurchaseResponse, HttpServletRequest request) {
    log.info("\n" + "결제완료 데이터 수신" + "\n" + "주문자 ID : " + webhookPurchaseResponse.getEvent().getOwnerMemberId() + "\n" + "박스 pk : " + webhookPurchaseResponse.getEvent().getSubscriberAttributes().getBoxPk().getValue());

    // 1. 토큰 검증
    verifyWebhookAuthorization(request);


    // 2. 멤버가 존재하는지 확인
    log.info("[2. 멤버가 존재하는지 확인]");
    String ownerMemberId = webhookPurchaseResponse.getEvent().getOwnerMemberId();
    Long ownerMemberIdToLong = Long.parseLong(ownerMemberId);
    Member member = memberRepository.findById(ownerMemberIdToLong)
        .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

    // 3. 박스가 존재하는지 확인
    log.info("[3. 박스가 존재하는지 확인]");
    String boxPk = webhookPurchaseResponse.getEvent().getSubscriberAttributes().getBoxPk().getValue();
    Long boxPkToLong = Long.parseLong(boxPk);
    Box box = boxRepository.findById(boxPkToLong)
        .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

    // 4. box_subscription 테이블 업데이트 또는 삽입
    log.info("[4. box_subscription 테이블 업데이트 또는 삽입]");
    String productId = webhookPurchaseResponse.getEvent().getProductId();
    PaymentStore store = webhookPurchaseResponse.getEvent().getStore();
    SubscriptionStatus subscriptionStatus = SubscriptionStatus.ACTIVE;
    LocalDateTime startedAt = LocalDateTime.now();
    LocalDateTime expiredAt = LocalDateTime.now().plusMonths(1);
    Double price = webhookPurchaseResponse.getEvent().getPrice();

    BoxSubscription boxSubscription = BoxSubscription
        .of(member, box, productId, store, subscriptionStatus, startedAt, expiredAt, price);

    SubscriptionEventLog subscriptionEventLog = SubscriptionEventLog.from(webhookPurchaseResponse, member, box);

    // 5. 박스 구독 정보 저장
    log.info("[5. 박스 구독 정보 저장]");
    if(boxSubscriptionRepository.findByBoxPk(boxPkToLong).isPresent()) {
      log.info("[5. 박스 구독 정보 저장] - 기존 구독 정보가 존재하여 업데이트를 진행합니다.(" + "구매한 box pk : " + boxPk + ")" + "(" + "구매자 pk : " + ownerMemberId + ")");
      BoxSubscription oldBoxSubscription = boxSubscriptionRepository.findByBoxPk(boxPkToLong)
          .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));
      BoxSubscription updatedBoxSubscription = oldBoxSubscription.replaceTo(boxSubscription);
      boxSubscriptionRepository.save(updatedBoxSubscription);
    } else {
      log.info("[5. 박스 구독 정보 저장] - 새로운 구독 정보를 저장합니다.(" + "구매한 box pk : " + boxPk + ")" + "(" + "구매자 pk : " + ownerMemberId + ")");
      boxSubscriptionRepository.save(boxSubscription);
    }

    // 6. 박스 구독 로그 저장
    log.info("[6. 박스 구독 로그 저장]");
    log.info("[6. 박스 구독 로그 저장] - 구매 로그를 저장합니다.(" + "구매한 box pk : " + boxPk + ")" + "(" + "구매자 pk : " + ownerMemberId + ")");
    subscriptionEventLogRepository.save(subscriptionEventLog);

    // 7. 최종 BOX 구독상태 변경
    log.info("[7. 최종 BOX 구독상태 변경]");
    box.subscribe();

  }

  @Transactional
  public void handleCancelWebhook(WebhookPurchaseResponse webhookPurchaseResponse, HttpServletRequest request) {
    verifyWebhookAuthorization(request);

    log.info("[cancel webhook] 수신 시작");

    validateWebhookType(webhookPurchaseResponse, PaymentEventType.CANCELLATION);

    String ownerMemberId = webhookPurchaseResponse.getEvent().getOwnerMemberId();
    Long memberPk = Long.parseLong(ownerMemberId);

    String boxPkValue = extractWebhookBoxPk(webhookPurchaseResponse);
    Long boxPk = Long.parseLong(boxPkValue);

    Member member = memberRepository.findById(memberPk)
            .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

    Box box = boxRepository.findActiveBoxByIdWithLock(boxPk)
            .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

    BoxSubscription boxSubscription = boxSubscriptionRepository
            .findByOwnerMemberIdAndBoxPk(memberPk, boxPk)
            .orElseGet(() -> createEmptySubscription(member, box));

    LocalDateTime expiredAt = toLocalDateTime(webhookPurchaseResponse.getEvent().getExpirationAtMs());

    boxSubscription.sync(
            webhookPurchaseResponse.getEvent().getProductId(),
            webhookPurchaseResponse.getEvent().getStore(),
            SubscriptionStatus.CANCELED,
            boxSubscription.getStartedAt(),
            expiredAt,
            webhookPurchaseResponse.getEvent().getPrice()
    );

    boxSubscriptionRepository.save(boxSubscription);

    // 취소는 아직 만료가 아니므로 premium 유지,cancel 상태로 변경
    box.subscribeCancel();
    boxRepository.save(box);

    SubscriptionEventLog eventLog = createWebhookLog(
            webhookPurchaseResponse,
            member,
            box,
            SubscriptionStatus.CANCELED,
            LocalDateTime.now(),
            null
    );
    subscriptionEventLogRepository.save(eventLog);

    log.info("[cancel webhook] 처리 완료 memberPk={}, boxPk={}, expiredAt={}", memberPk, boxPk, expiredAt);
  }

  @Transactional
  public void handleUncancelWebhook(WebhookPurchaseResponse webhookPurchaseResponse, HttpServletRequest request) {
    verifyWebhookAuthorization(request);

    log.info("[uncancel webhook] 수신 시작");

    validateWebhookType(webhookPurchaseResponse, PaymentEventType.UNCANCELLATION);

    String ownerMemberId = webhookPurchaseResponse.getEvent().getOwnerMemberId();
    Long memberPk = Long.parseLong(ownerMemberId);

    String boxPkValue = extractWebhookBoxPk(webhookPurchaseResponse);
    Long boxPk = Long.parseLong(boxPkValue);

    Member member = memberRepository.findById(memberPk)
            .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

    Box box = boxRepository.findActiveBoxByIdWithLock(boxPk)
            .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

    BoxSubscription boxSubscription = boxSubscriptionRepository
            .findByOwnerMemberIdAndBoxPk(memberPk, boxPk)
            .orElseGet(() -> createEmptySubscription(member, box));

    LocalDateTime expiredAt = toLocalDateTime(webhookPurchaseResponse.getEvent().getExpirationAtMs());

    boxSubscription.sync(
            webhookPurchaseResponse.getEvent().getProductId(),
            webhookPurchaseResponse.getEvent().getStore(),
            SubscriptionStatus.ACTIVE,
            boxSubscription.getStartedAt(),
            expiredAt,
            webhookPurchaseResponse.getEvent().getPrice()
    );

    boxSubscriptionRepository.save(boxSubscription);

    box.subscribe();
    boxRepository.save(box);

    SubscriptionEventLog eventLog = createWebhookLog(
            webhookPurchaseResponse,
            member,
            box,
            SubscriptionStatus.ACTIVE,
            null,
            null
    );
    subscriptionEventLogRepository.save(eventLog);

    log.info("[uncancel webhook] 처리 완료 memberPk={}, boxPk={}", memberPk, boxPk);
  }

  @Transactional
  public PaymentSyncResponse syncPayment(Long memberPk, Long boxPk) {
    log.info("[결제 동기화 시작] memberPk={}, boxPk={}", memberPk, boxPk);

    Member member = memberRepository.findById(memberPk)
            .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
    log.info("[sync] member 조회 완료 memberPk={}", memberPk);

    Box box = boxRepository.findActiveBoxByIdWithLock(boxPk)
            .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));
    log.info("[sync] box 조회 완료 boxPk={}", boxPk);

    validateBoxOwner(memberPk, box);
    log.info("[sync] box owner 검증 완료");

    String appUserId = String.valueOf(memberPk);

    RevenueCatSubscriberResponse rcResponse = revenueCatClient.getSubscriber(appUserId);
    if (rcResponse == null || rcResponse.getSubscriber() == null) {
      log.error("[sync] RevenueCat subscriber 조회 실패 appUserId={}", appUserId);
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }
    log.info("[sync] RevenueCat subscriber 조회 성공 appUserId={}", appUserId);

    RevenueCatSubscriberResponse.Subscriber subscriber = rcResponse.getSubscriber();

    String rcBoxPk = extractBoxPk(subscriber);
    log.info("[sync] RevenueCat subscriber attribute boxPk={}", rcBoxPk);

    validateRequestedBox(boxPk, rcBoxPk);

    RevenueCatSubscriberResponse.Entitlement premiumEntitlement =
            extractPremiumEntitlement(subscriber.getEntitlements());

    boolean premiumActive = premiumEntitlement != null;
    log.info("[sync] entitlement 확인 premiumActive={}", premiumActive);

    LocalDateTime startedAt = null;
    LocalDateTime expiredAt = null;
    String productId = null;
    Double price = null;

    if (premiumActive) {
      startedAt = parseDateTime(premiumEntitlement.getPurchaseDate());
      expiredAt = parseDateTime(premiumEntitlement.getExpiresDate());
      productId = premiumEntitlement.getProductIdentifier();
      log.info("[sync] entitlement detail productId={}, startedAt={}, expiredAt={}",
              productId, startedAt, expiredAt);
    }

    BoxSubscription boxSubscription = boxSubscriptionRepository
            .findByOwnerMemberIdAndBoxPk(memberPk, boxPk)
            .orElseGet(() -> createEmptySubscription(member, box));
    log.info("[sync] subscription 조회 또는 생성 완료");

    boxSubscription = updateSubscription(
            boxSubscription,
            productId,
            PaymentStore.APP_STORE,
            premiumActive,
            startedAt,
            expiredAt,
            price
    );

    boxSubscriptionRepository.save(boxSubscription);
    log.info("[sync] subscription 저장 완료");

    updateBoxPremium(box, premiumActive);

    saveSyncLog(
            member,
            box,
            productId,
            PaymentStore.APP_STORE,
            price,
            premiumActive,
            null,
            String.valueOf(memberPk)
    );
    log.info("[sync] 결제 동기화 로그 저장 완료");

    return PaymentSyncResponse.builder()
            .boxPk(boxPk)
            .premium(premiumActive)
            .productId(productId)
            .store(PaymentStore.APP_STORE.name())
            .startedAt(startedAt)
            .expiredAt(expiredAt)
            .syncedFrom("MANUAL_SYNC")
            .build();
  }

  private void validateBoxOwner(Long memberPk, Box box) {
    if (box.getOwnerPk() == null || !box.getOwnerPk().equals(memberPk)) {
      throw new ValidationException(ErrorCode.FORBIDDEN);
    }
  }

  private String extractBoxPk(RevenueCatSubscriberResponse.Subscriber subscriber) {
    Map<String, RevenueCatSubscriberResponse.SubscriberAttribute> attrs = subscriber.getSubscriberAttributes();
    if (attrs == null || !attrs.containsKey("boxPk") || attrs.get("boxPk") == null) {
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }
    return attrs.get("boxPk").getValue();
  }

  private void validateRequestedBox(Long requestedBoxPk, String rcBoxPk) {
    if (rcBoxPk == null || rcBoxPk.isBlank()) {
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }

    try {
      if (!requestedBoxPk.equals(Long.parseLong(rcBoxPk))) {
        throw new ValidationException(ErrorCode.INVALID_REQUEST);
      }
    } catch (NumberFormatException e) {
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }
  }

  private RevenueCatSubscriberResponse.Entitlement extractPremiumEntitlement(
          Map<String, RevenueCatSubscriberResponse.Entitlement> entitlements) {

    if (entitlements == null || entitlements.isEmpty()) {
      return null;
    }

    return entitlements.get("premium");
  }

  private LocalDateTime parseDateTime(String dateTime) {
    if (dateTime == null) {
      return null;
    }
    return OffsetDateTime.parse(dateTime)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime();
  }

  private BoxSubscription createEmptySubscription(Member member, Box box) {
    return BoxSubscription.of(
            member,
            box,
            null,
            null,
            SubscriptionStatus.EXPIRED,
            null,
            null,
            null
    );
  }

  private BoxSubscription updateSubscription(
          BoxSubscription subscription,
          String productId,
          PaymentStore store,
          boolean premiumActive,
          LocalDateTime startedAt,
          LocalDateTime expiredAt,
          Double price
  ) {
    subscription.sync(
            productId,
            store,
            premiumActive ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED,
            startedAt,
            expiredAt,
            price
    );

    return subscription;
  }

  private void updateBoxPremium(Box box, boolean premiumActive) {
    if (premiumActive) {
      box.subscribe();
    } else {
      box.unsubscribe();
    }
    boxRepository.save(box);
  }

  private void saveSyncLog(
          Member member,
          Box box,
          String productId,
          PaymentStore store,
          Double price,
          boolean premiumActive,
          String appId,
          String appUserId
  ) {

    SubscriptionEventLog logEntity = SubscriptionEventLog.of(
            member,
            box,
            productId,
            store,
            premiumActive ? SubscriptionStatus.ACTIVE : SubscriptionStatus.EXPIRED,
            null,           // cancelledAt
            null,           // refundedAt
            price,
            appId,
            appUserId,
            premiumActive ? PaymentEventType.INITIAL_PURCHASE : PaymentEventType.EXPIRATION,
            null            // payload (sync에서는 raw payload 없음)
    );

    subscriptionEventLogRepository.save(logEntity);
  }

  /**
   * HTTP 요청 헤더에서 JWT 토큰을 추출하는 메서드입니다.
   *
   * @param request HTTP 요청
   * @return 추출된 JWT 토큰
   */
  public String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");

    // Authorization 헤더가 없거나 "Bearer "로 시작하지 않는 경우 예외 처리
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.error("[1. 토큰 검증] Authorization 헤더가 없거나 Bearer 로 시작하지 않습니다.");
      throw new ValidationException(ErrorCode.TOKEN_EXTRACTION_FAILED);
    }

    String token = authHeader.substring(7);

    return token;
  }

  /**
   * 구매 로그를 조회하는 메서드입니다.
   *
   * @param bokPk 조회하고자 하는 박스 pk 입니다.
   * @return 정제된 구매 로그 리스트 입니다.
   */
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

  private void verifyWebhookAuthorization(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || authHeader.isBlank()) {
      log.error("[webhook auth] Authorization 헤더 없음");
      throw new ValidationException(ErrorCode.TOKEN_EXTRACTION_FAILED);
    }

    if (!authHeader.equals(revenueCatWebhookAuthorization)) {
      log.error("[webhook auth] Authorization 불일치");
      throw new ValidationException(ErrorCode.TOKEN_INVALID);
    }
  }

  private void validateWebhookType(WebhookPurchaseResponse response, PaymentEventType expectedType) {
    if (response == null || response.getEvent() == null || response.getEvent().getType() == null) {
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }

    if (response.getEvent().getType() != expectedType) {
      log.error("[webhook type] expectedType={}, actualType={}", expectedType, response.getEvent().getType());
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }
  }

  private String extractWebhookBoxPk(WebhookPurchaseResponse response) {
    if (response == null
            || response.getEvent() == null
            || response.getEvent().getSubscriberAttributes() == null
            || response.getEvent().getSubscriberAttributes().getBoxPk() == null
            || response.getEvent().getSubscriberAttributes().getBoxPk().getValue() == null
            || response.getEvent().getSubscriberAttributes().getBoxPk().getValue().isBlank()) {
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }

    return response.getEvent().getSubscriberAttributes().getBoxPk().getValue();
  }

  private LocalDateTime toLocalDateTime(Long epochMillis) {
    if (epochMillis == null) {
      return null;
    }

    return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(epochMillis),
            ZoneId.systemDefault()
    );
  }

  private SubscriptionEventLog createWebhookLog(
          WebhookPurchaseResponse webhookPurchaseResponse,
          Member member,
          Box box,
          SubscriptionStatus status,
          LocalDateTime cancelledAt,
          LocalDateTime refundedAt
  ) {
    try {
      return SubscriptionEventLog.of(
              member,
              box,
              webhookPurchaseResponse.getEvent().getProductId(),
              webhookPurchaseResponse.getEvent().getStore(),
              status,
              cancelledAt,
              refundedAt,
              webhookPurchaseResponse.getEvent().getPrice(),
              webhookPurchaseResponse.getEvent().getAppId(),
              webhookPurchaseResponse.getEvent().getOwnerMemberId(),
              webhookPurchaseResponse.getEvent().getType(),
              objectMapper.writeValueAsString(webhookPurchaseResponse)
      );
    } catch (Exception e) {
      log.error("[webhook log] payload 직렬화 실패", e);
      throw new ValidationException(ErrorCode.INVALID_REQUEST);
    }
  }

}
