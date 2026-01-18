package com.fitian.burntz.domain.alarm.adapter;

import com.fitian.burntz.domain.alarm.port.FirestorePushPort;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.adapter
 * @fileName : FirestorePushAdapter
 * @date : 2026-01-18
 * @description :
 */

@Component
@RequiredArgsConstructor
public class FirestorePushAdapter implements FirestorePushPort {
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";

    // PROCESSING이 오래 지속되면 "좀비 락"으로 보고 takeover 허용
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(2);

    private final Firestore firestore;

    @Override
    public boolean acquireDispatch(String boxCode, String channelId, String messageId) {
        DocumentReference ref = dispatchDoc(boxCode, channelId, messageId);

        try {
            return Boolean.TRUE.equals(
                    firestore.runTransaction(tx -> {
                        DocumentSnapshot snap = tx.get(ref).get();
                        Timestamp now = Timestamp.now();

                        // 없으면 최초 생성(선점 성공)
                        if (!snap.exists()) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("status", STATUS_PROCESSING);
                            data.put("createdAt", now);
                            data.put("updatedAt", now);
                            tx.create(ref, data);
                            return true;
                        }

                        String status = snap.getString("status");
                        Timestamp updatedAt = snap.getTimestamp("updatedAt");

                        // 이미 SENT면 멱등 처리로 종료
                        if (STATUS_SENT.equals(status)) {
                            return false;
                        }

                        // PROCESSING인데 오래되었으면 takeover 허용
                        if (STATUS_PROCESSING.equals(status) && isStale(updatedAt, now)) {
                            Map<String, Object> patch = new HashMap<>();
                            patch.put("status", STATUS_PROCESSING);
                            patch.put("updatedAt", now);
                            patch.put("takeover", true);
                            tx.update(ref, patch);
                            return true;
                        }

                        // FAILED 재시도 허용(원치 않으면 false로 변경)
                        if (STATUS_FAILED.equals(status)) {
                            Map<String, Object> patch = new HashMap<>();
                            patch.put("status", STATUS_PROCESSING);
                            patch.put("updatedAt", now);
                            patch.put("retry", true);
                            tx.update(ref, patch);
                            return true;
                        }

                        return false;
                    }).get()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore transaction interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Firestore transaction failed", e);
        }
    }

    @Override
    public void markSent(String boxCode, String channelId, String messageId) {
        DocumentReference ref = dispatchDoc(boxCode, channelId, messageId);

        Map<String, Object> patch = new HashMap<>();
        patch.put("status", STATUS_SENT);
        patch.put("sentAt", Timestamp.now());
        patch.put("updatedAt", Timestamp.now());

        ref.set(patch, SetOptions.merge());
    }

    @Override
    public void markFailed(String boxCode, String channelId, String messageId, String reason) {
        DocumentReference ref = dispatchDoc(boxCode, channelId, messageId);

        Map<String, Object> patch = new HashMap<>();
        patch.put("status", STATUS_FAILED);
        patch.put("failReason", reason);
        patch.put("updatedAt", Timestamp.now());

        ref.set(patch, SetOptions.merge());
    }

    /**
     * ✅ 핵심 로직
     * participants/{memberPk} 문서들 중
     * - lastReadAt < sentAt  (아직 읽지 않음)
     * - lastReadAt == null   (한번도 읽지 않았거나 필드 없음)
     * 인 문서의 docId를 Long(memberPk)으로 파싱하여 반환
     */
    @Override
    public List<Long> findMemberPksToNotify(String boxCode, String channelId, LocalDateTime sentAt) {
        Timestamp sentAtTs = toTimestampKst(sentAt);
        CollectionReference participants = participantsCol(boxCode, channelId);

        try {
            List<Long> result = new ArrayList<>();

            // 1) lastReadAt < sentAt 인 사용자
            QuerySnapshot q1 = participants
                    .whereLessThan("lastReadAt", sentAtTs)
                    .get()
                    .get();

            for (DocumentSnapshot doc : q1.getDocuments()) {
                parseDocIdAsLong(doc.getId()).ifPresent(result::add);
            }

            // 2) lastReadAt 필드가 없는/NULL 인 사용자도 대상(정책)
            // - Firestore는 "필드 없음"을 직접 where로 잡기 애매할 수 있음
            // - 다만 whereEqualTo("lastReadAt", null)로 잡히는 환경이면 포함됨
            QuerySnapshot q2 = participants
                    .whereEqualTo("lastReadAt", null)
                    .get()
                    .get();

            for (DocumentSnapshot doc : q2.getDocuments()) {
                parseDocIdAsLong(doc.getId()).ifPresent(result::add);
            }

            // 중복 제거 후 반환
            return result.stream().distinct().toList();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Firestore query interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Firestore query failed", e);
        }
    }

    // ---------------------------
    // 내부 유틸
    // ---------------------------

    private boolean isStale(Timestamp updatedAt, Timestamp now) {
        if (updatedAt == null) return true;
        Instant u = Instant.ofEpochSecond(updatedAt.getSeconds(), updatedAt.getNanos());
        Instant n = Instant.ofEpochSecond(now.getSeconds(), now.getNanos());
        return Duration.between(u, n).compareTo(PROCESSING_TTL) > 0;
    }

    private Optional<Long> parseDocIdAsLong(String docId) {
        try {
            return Optional.of(Long.parseLong(docId));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Timestamp toTimestampKst(LocalDateTime ldt) {
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("Asia/Seoul"));
        Instant instant = zdt.toInstant();
        return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
    }

    private DocumentReference dispatchDoc(String boxCode, String channelId, String messageId) {
        return firestore.collection("boxes").document(boxCode)
                .collection("channels").document(channelId)
                .collection("message_dispatch").document(messageId);
    }

    private CollectionReference participantsCol(String boxCode, String channelId) {
        return firestore.collection("boxes").document(boxCode)
                .collection("channels").document(channelId)
                .collection("participants");
    }
}
