package com.fitian.burntz.domain.record.v1.controller;

import com.fitian.burntz.domain.record.service.RankingQueryService;
import com.fitian.burntz.domain.record.v1.dto.RankingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.v1.controller
 * @fileName : RankingController
 * @date : 2025-09-25
 * @description :
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/boxes/{boxPk}/wod/{date}")
public class RankingController {

    private final RankingQueryService rankingQueryService;

    @GetMapping("/ranking")
    public RankingResponse getRanking(@PathVariable Long boxPk, @PathVariable String date) {
        return rankingQueryService.getDailyRanking(boxPk, LocalDate.parse(date));
    }

//    @GetMapping("/ranking")
//    public RankingResponse getRanking(@PathVariable Long boxPk,
//                                      @PathVariable String date,
//                                      @RequestParam(value="source", required=false) String source) {
//        LocalDate d = LocalDate.parse(date);
//        if ("db".equalsIgnoreCase(source)) {
//            return rankingQueryService.getDailyRankingFromDbOnly(boxPk, d); // 아래 메서드 추가
//        }
//        return rankingQueryService.getDailyRanking(boxPk, d); // 기존: Redis 우선
//    }
}