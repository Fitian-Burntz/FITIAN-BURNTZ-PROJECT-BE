package com.fitian.burntz.domain.record.v1.dto;

import com.fitian.burntz.domain.record.service.RankingService.RankingRow;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
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
    private String wodType;   // 예: "ForTime"
    private int count;
    private List<Item> ranking;

    @Getter @AllArgsConstructor
    public static class Item {
        private int rank;
        private Long recordPk;

        // Record 엔티티의 값들
        private Long wodPk;
        private Long classesPk;
        private Long memberListPk; // 게스트 null
        private String nickname;   // record.nickname 우선, 없으면 memberList.boxNickname
        private String level;      // "rx'd", "A", "B", "C" (원문 그대로)
        private Integer round;
        private Integer reps;
        private Float time;
        private String result;     // "S"/"F" 또는 enum name
        private String team;
        private String memo;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}