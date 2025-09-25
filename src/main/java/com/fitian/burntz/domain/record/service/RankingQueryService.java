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
    private final RankingScoreEncoder encoder;
    private final RecordRepository recordRepository;
    private final WodRespository wodRepository;

    public RankingResponse getDailyRanking(Long boxPk, LocalDate date) {
        Wod wod = wodRepository.findByBoxBoxPkAndWodDateAndDeletedYN(boxPk, date, BaseTime.Yn.N)
                .orElseThrow(() -> new IllegalArgumentException("WOD_NOT_FOUND"));
        WodType type = wod.getWodType();
        List<RankingRow> rows = rankingService.getRanking(boxPk, date, () -> rebuildFromDb(boxPk, date, type));
        return RankingResponse.from(boxPk, date.toString(), type.name(), rows);
    }

    private List<RankingRow> rebuildFromDb(Long boxPk, LocalDate date, WodType type) {
        List<Record> records;
        switch (type) {
            case ForTime    -> records = recordRepository.findForTimeOrder(boxPk, date);
            case AMRAP      -> records = recordRepository.findAmrapOrder(boxPk, date);
            case EMOM, SuccessFail -> records = recordRepository.findEmomOrSfOrder(boxPk, date);
            case EMOMMAX, MaxReps  -> records = recordRepository.findMaxRepsOrder(boxPk, date);
            default -> records = List.of();
        }

        List<RankingRow> out = new ArrayList<>(records.size());
        int rank = 1;
        for (Record r : records) {
            double score = encoder.scoreFor(type, r.getLevel(), r.getRound(), r.getReps(), r.getTime(), r.getResult());
            String mem = encoder.memberString(
                    r.getLevel(),
                    encoder.metricKey(type, r.getRound(), r.getReps(), r.getTime(), r.getResult()),
                    (r.getNickname()!=null ? r.getNickname()
                            : (r.getMemberList()!=null ? r.getMemberList().getBoxNickname() : "")),
                    r.getRecordPk(),
                    (r.getMemberList()==null ? null : r.getMemberList().getMemberListPk())
            );
            RankingRow row = RankingRow.fromRankAndMember(rank++, mem);
            row.setScore(score);
            row.setMember(mem);
            out.add(row);
        }
        return out;
    }

    public RankingResponse getDailyRankingFromDbOnly(Long boxPk, LocalDate date) {
        var wod = wodRepository.findByBoxBoxPkAndWodDateAndDeletedYN(boxPk, date, BaseTime.Yn.N)
                .orElseThrow(() -> new IllegalArgumentException("WOD_NOT_FOUND"));
        var rows = rebuildFromDb(boxPk, date, wod.getWodType()); // 기존 재빌드 메서드 재사용
        return RankingResponse.from(boxPk, date.toString(), wod.getWodType().name(), rows);
    }
}