package com.fitian.burntz.domain.record.ranking;

import com.fitian.burntz.domain.record.enums.RecordResult;
import com.fitian.burntz.domain.wod.enums.WodType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
/* 운동 기록을 숫자 점수로 변환하는 클래스 */
public class RankingScoreEncoder {

    private static final double LEVEL_GAP = 100_000_000.0;  //레벨 간 간격
    private static final double MAX_TIME = 86400.0;         //최대 시간(24시간)
    private static final double MAX_ROUND = 10000.0;        //최대 라운드
    private static final double MAX_REPS = 10000.0;         //최대 렙스

    /**
     * 레벨을 숫자로 변환
     * Rx'd(0) > Rx'd/A(1) > Rx'd/B(2) > Rx'd/C(3) > Rx'd/S(4)
     * > A(5) > A/B(6) > A/C(7) > A/S(8)
     * > B(9) > B/C(10) > B/S(11)
     * > C(12) > C/S(13) > Scale(14) > 그외(15)
     */
    private int getLevelRank(String level) {
        if (level == null) return 15;
        return switch (level.trim().toLowerCase()) {
            case "rx'd", "rxd", "rx" -> 0;
            case "rx'd/a" -> 1;
            case "rx'd/b" -> 2;
            case "rx'd/c" -> 3;
            case "rx'd/s" -> 4;
            case "a" -> 5;
            case "a/b" -> 6;
            case "a/c" -> 7;
            case "a/s" -> 8;
            case "b" -> 9;
            case "b/c" -> 10;
            case "b/s" -> 11;
            case "c" -> 12;
            case "c/s" -> 13;
            case "scale" -> 14;
            default -> 15;
        };
    }

    /**
     * 운동 기록을 점수로 변환
     */
    public double scoreFor(WodType type, String level, Integer rounds, Integer reps,
                           Float time, RecordResult result) {
        //base : 레벨순위 x 100,000,000
        double base = getLevelRank(level) * LEVEL_GAP;

        if (type == null) return base + LEVEL_GAP - 1;

        switch (type) {
            case ForTime:   //시간이 적을수록 좋음-> 즉, 점수가 낮을수록 순위가 높음
                return base + (time != null ? time : MAX_TIME);
            /*
             * Rx'd 레벨, 180초 → 0 + 180 = 180점
             * Rx'd 레벨, 200초 → 0 + 200 = 200점
             * A 레벨, 150초 → 100,000,000 + 150 = 100,000,150점
             * */

            case AMRAP: //라운드와 렙스가 많을 수록 좋음
                double r = rounds != null ? rounds : 0;
                double p = reps != null ? reps : 0;
                return base + ((MAX_ROUND - r) * 10000) + (MAX_REPS - p);
            /*
             * Rx'd, 5라운드, 20 렙스
             * = 0 + (10000-5)×10000 + (10000-20)
             * = 0 + 99,950,000 + 9,980 = 99,959,980점
             *
             * Rx'd, 6라운드, 10렙스:
             * = 0 + (10000-6)×10000 + (10000-10)
             * = 0 + 99,940,000 + 9,990 = 99,949,990점  ← 점수가 더 낮음 = 순위가 더 높음!
             * */

            case EMOM:
            case SuccessFail:   //성공 -> 0, 실패 -> 1
                return base + ((result == RecordResult.S) ? 0 : 1);

            case EMOMMAX:
            case MaxReps:   //렙스가 많을수록 좋음
                return base + (MAX_REPS - (reps != null ? reps : 0));
            /*
            * Rx'd, 100렙스: 0 + (10000-100) = 9900점
            * Rx'd, 150렙스: 0 + (10000-150) = 9850점 ← 더 높은 순위
            * */

            default:
                return base + LEVEL_GAP - 1;
        }
    }


    /**
     * Redis member 문자열 생성 (동점 해소용)
     * "level=0|nickname=홍길동|memberListPk=000000000042|recordPk=1001"
     */
    public String memberString(String level, String nickname, Long recordPk, Long memberListPk) {
        String nick = nickname != null ? nickname.trim() : "";
        String ml = String.format("%06d", memberListPk != null ? memberListPk : 0);
        return String.format("level=%d|nickname=%s|memberListPk=%s|recordPk=%d",
                getLevelRank(level), nick, ml, recordPk);
    }

    /**
     * member 문자열에서 recordPk 추출
     */
    public Long extractRecordPk(String member) {
        // "level=0|nickname=...|memberListPk=...|recordPk=456" → 456 추출
        String[] parts = member.split("\\|");
        return Long.valueOf(parts[3].substring(9)); // "recordPk=" 제거
    }
}