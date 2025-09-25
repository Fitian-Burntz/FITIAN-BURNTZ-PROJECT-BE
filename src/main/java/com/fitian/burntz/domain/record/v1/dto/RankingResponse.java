package com.fitian.burntz.domain.record.v1.dto;

import com.fitian.burntz.domain.record.service.RankingService.RankingRow;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.v1.dto
 * @fileName : RankingResponse
 * @date : 2025-09-25
 * @description : 랭킹 응답 DTO
 */
@Getter
@AllArgsConstructor
public class RankingResponse {
    private Long boxPk;
    private String date;
    private String wodType;
    private int count;
    private List<Item> ranking;

    @Getter
    @AllArgsConstructor
    public static class Item {
        private int rank;
        private Long recordPk;
        private Long memberListPk; // 게스트 null
        private String nickname;
        private String level;
        private String metric;
    }

    public static RankingResponse from(Long boxPk, String date, String wodType, List<RankingRow> rows){
        return new RankingResponse(
                boxPk, date, wodType, rows.size(),
                rows.stream().map(r ->
                        new Item(r.getRank(), r.getRecordPk(), r.getMemberListPk(), r.getNickname(), r.getLevel(), r.getMetric())
                ).toList()
        );
    }
}