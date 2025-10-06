package com.fitian.burntz.domain.member.service;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.member.service
 * @fileName : BoxNicknameEventListener
 * @date : 2025-10-06
 * @description : 이벤트 리스너
 */
import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.record.repository.RecordRepository;
import com.fitian.burntz.domain.record.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoxNicknameEventListener {

    private final RecordRepository recordRepository;
    private final RankingService rankingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNicknameChanged(BoxNicknameChangedEvent event) {
        try {
            // DB에서 Record 조회 (트랜잭션 커밋 후라 최신 데이터)
            List<Record> records = recordRepository.findAllByRecordPkInWithJoins(
                    event.getRecordPks()
            );

            // 날짜별로 그룹핑 (같은 Redis 키에 대해 한 번에 처리)
            Map<String, List<Record>> recordsByKey = records.stream()
                    .collect(Collectors.groupingBy(r ->
                            String.format("rk:%d:%s",
                                    r.getWod().getBox().getBoxPk(),
                                    r.getWod().getWodDate())
                    ));

            // 키별로 파이프라인으로 한 번에 처리
            int totalUpdated = 0;
            for (Map.Entry<String, List<Record>> entry : recordsByKey.entrySet()) {
                rankingService.updateNicknamesBatch(
                        entry.getValue(),
                        event.getOldNickname(),
                        event.getMemberListPk()
                );
                totalUpdated += entry.getValue().size();
            }

            log.info("Redis 랭킹 업데이트 완료: {} records, {} keys",
                    totalUpdated, recordsByKey.size());

        } catch (Exception e) {
            log.error("Redis 랭킹 업데이트 실패: {}", e.getMessage(), e);
            // Redis 실패는 무시 (다음 조회 시 rebuild됨)
        }
    }
}
