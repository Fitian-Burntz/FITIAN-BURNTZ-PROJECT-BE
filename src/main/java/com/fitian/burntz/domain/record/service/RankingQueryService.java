package com.fitian.burntz.domain.record.service;

import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.record.ranking.RankingScoreEncoder;
import com.fitian.burntz.domain.record.repository.RecordRepository;
import com.fitian.burntz.domain.record.v1.dto.RankingResponse;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.domain.wod.enums.WodType;
import com.fitian.burntz.domain.wod.repository.WodRespository;
import com.fitian.burntz.global.common.entity.BaseTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fitian.burntz.domain.record.service.RankingService.RankingRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.service
 * @fileName : RankingQueryService
 * @date : 2025-09-25
 * @description : 랭킹쿼리관련 service
 */
@Service
@RequiredArgsConstructor
public class RankingQueryService {

    private final RankingService rankingService;
    private final RecordRepository recordRepository;
    private final WodRespository wodRepository;
    private final RankingScoreEncoder encoder;

    /** Redis 우선 → (미스/장애) DB 폴백 → rows 확보 → DB에서 레코드 확장 → 응답 */
    public RankingResponse getDailyRanking(Long boxPk, LocalDate date) {
        Wod wod = wodRepository.findByBoxBoxPkAndWodDateAndDeletedYN(boxPk, date, BaseTime.Yn.N)
                .orElseThrow(() -> new IllegalArgumentException("WOD_NOT_FOUND"));
        WodType type = wod.getWodType();

        List<RankingRow> rows = rankingService.getRanking(boxPk, date,
                () -> rebuildFromDb(boxPk, date, type)); // 기존 재빌드 로직 재사용(그 안에서 score/member 채움)

        return expandRowsToResponse(boxPk, date, type, rows);
    }

    /** (옵션) 캐시 무시 DB 전용 */
    public RankingResponse getDailyRankingFromDbOnly(Long boxPk, LocalDate date) {
        Wod wod = wodRepository.findByBoxBoxPkAndWodDateAndDeletedYN(boxPk, date, BaseTime.Yn.N)
                .orElseThrow(() -> new IllegalArgumentException("WOD_NOT_FOUND"));
        WodType type = wod.getWodType();

        List<RankingRow> rows = rebuildFromDb(boxPk, date, type);
        return expandRowsToResponse(boxPk, date, type, rows);
    }

    // ===== DB ORDER BY → ZSET 재적재용 rows (기존 코드 유지) =====
    private List<RankingService.RankingRow> rebuildFromDb(Long boxPk, LocalDate date, WodType type) {
        // 1) 타입별 ORDER BY로 DB에서 정렬된 레코드 조회
        List<Record> records;
        switch (type) {
            case ForTime -> records = recordRepository.findForTimeOrder(boxPk, date);
            case AMRAP -> records = recordRepository.findAmrapOrder(boxPk, date);
            case EMOM, SuccessFail -> records = recordRepository.findEmomOrSfOrder(boxPk, date);
            case EMOMMAX, MaxReps -> records = recordRepository.findMaxRepsOrder(boxPk, date);
            default -> records = List.of();
        }

        // 2) Redis 적재에 필요한 score/member 생성 + 랭킹 행으로 변환
        List<RankingService.RankingRow> out = new ArrayList<>(records.size());
        int rank = 1;
        for (Record r : records) {
            double score = encoder.scoreFor(
                    type, r.getLevel(), r.getRound(), r.getReps(), r.getTime(), r.getResult());

            String nickname = (r.getNickname() != null) ? r.getNickname()
                    : (r.getMemberList() != null ? r.getMemberList().getBoxNickname() : "");

            String member = encoder.memberString(
                    r.getLevel(),
                    encoder.metricKey(type, r.getRound(), r.getReps(), r.getTime(), r.getResult()),
                    nickname,
                    r.getRecordPk(),
                    (r.getMemberList() == null ? null : r.getMemberList().getMemberListPk())
            );

            RankingService.RankingRow row = RankingService.RankingRow.fromRankAndMember(rank++, member);
            row.setScore(score);   // 재적재할 때 사용
            row.setMember(member); // 재적재할 때 사용
            out.add(row);
        }
        return out;
    }

    // ===== 핵심: rows(recordPk 순서) -> Record들 한 번에 조회 -> 응답 아이템으로 확장 =====
    private RankingResponse expandRowsToResponse(Long boxPk, LocalDate date, WodType type, List<RankingRow> rows) {
        List<Long> ids = rows.stream().map(RankingRow::getRecordPk).toList();
        if (ids.isEmpty()) {
            return new RankingResponse(boxPk, date.toString(), type.name(), 0, List.of());
        }

        List<Record> records = recordRepository.findAllByRecordPkInWithJoins(ids);
        Map<Long, Record> byId = records.stream()
                .collect(Collectors.toMap(Record::getRecordPk, Function.identity()));

        List<RankingResponse.Item> items = new ArrayList<>(rows.size());
        for (RankingRow r : rows) {
            Record rec = byId.get(r.getRecordPk());
            if (rec == null) continue; // 혹시 중간에 삭제됐다면 스킵

            Long memberListPk = rec.getMemberList() == null ? null : rec.getMemberList().getMemberListPk();
            String nickname = rec.getNickname() != null ? rec.getNickname()
                    : (rec.getMemberList() != null ? rec.getMemberList().getBoxNickname() : "");

            items.add(new RankingResponse.Item(
                    r.getRank(),
                    rec.getRecordPk(),
                    rec.getWod().getWodPk(),
                    rec.getClasses().getClassesPk(),
                    memberListPk,
                    nickname,
                    rec.getLevel(),                // ← 원문 그대로 (“rx'd”, “A”, …)
                    rec.getRound(),
                    rec.getReps(),
                    rec.getTime(),
                    rec.getResult() == null ? null : rec.getResult().name(), // "S"/"F" 등
                    rec.getTeam(),
                    rec.getMemo(),
                    rec.getCreatedAt(),
                    rec.getUpdatedAt()
            ));
        }

        return new RankingResponse(boxPk, date.toString(), type.name(), items.size(), items);
    }
}