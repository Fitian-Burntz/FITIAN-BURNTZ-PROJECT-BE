package com.fitian.burntz.domain.record.ranking;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.ranking
 * @fileName : RankingScoreEncoder
 * @date : 2025-09-25
 * @description : 점수 인코더
 */

import com.fitian.burntz.domain.record.enums.RecordResult;
import com.fitian.burntz.domain.wod.enums.WodType;
import org.springframework.stereotype.Component;

/**
 * 레벨 → (타입별 지표 필드) → 동점 시 닉네임 → memberListPk → recordPk 
 * 순으로 정렬되도록 "score(숫자)" + "member(문자)"를 만들어 ZSET에 넣는 로직
 */
@Component
public class RankingScoreEncoder {

    /** 레벨 우선순위: rx'd(0) < A(1) < B(2) < C(3) */
    public enum LevelRank {
        RXD(0), A(1), B(2), C(3);
        public final int v;
        LevelRank(int v) { this.v = v; }
        public static int of(String level){
            if (level == null) return 9; // 정의 밖은 맨 뒤
            String s = level.trim().toLowerCase();
            if (s.equals("rx'd") || s.equals("rxd") || s.equals("rx")) return RXD.v;
            if (s.equals("a")) return A.v;
            if (s.equals("b")) return B.v;
            if (s.equals("c")) return C.v;
            return 9;
        }
    }

    /** 타입별 지표 숫자화에 쓸 상한(역변환용). 여유있게 크게. */
    private static final long R_MAX   = 1_000_000L;
    private static final long REP_MAX = 1_000_000L;

    /** 레벨 영역을 완전히 분리하기 위한 step. (double 정밀도 안전 영역) */
    private static final double LVL_STEP = 1_000_000_000_000.0; // 1e12

    /**
     * 하나의 기록을 오름차순 정렬 가능한 단일 score로 만든다.
     * - ForTime: 시간 낮을수록 상위 → ms 그대로 더함
     * - AMRAP: 라운드/렙스 높을수록 상위 → 역변환해 작은 수가 상위가 되도록
     * - EMOM/SF: S(0) < F(1)
     * - MAXREPS/EMOMMAX: 렙스 높을수록 상위 → 역변환
     */

    public double scoreFor(WodType wodType, String level, Integer rounds, Integer reps,
                           Float timeSeconds, RecordResult result) {
        return scoreFor(wodType == null ? null : wodType.name(),
                level, rounds, reps, timeSeconds,
                result == null ? null : result.name());
    }
    public String metricKey(WodType wodType, Integer rounds, Integer reps,
                            Float timeSeconds, RecordResult result) {
        return metricKey(wodType == null ? null : wodType.name(),
                rounds, reps, timeSeconds,
                result == null ? null : result.name());
    }

    public double scoreFor(String wodType, String level, Integer rounds, Integer reps,
                           Float timeSeconds, String result) {
        int lvl = LevelRank.of(level);
        double base = lvl * LVL_STEP;
        String t = wodType == null ? "" : wodType.trim().toUpperCase();

        switch (t) {
            case "FORTIME": {
                long ms = (timeSeconds == null) ? Long.MAX_VALUE/2
                        : (long)Math.round(timeSeconds * 1000.0);
                return base + ms; // 낮을수록 상위
            }
            case "AMRAP": {
                long r = (rounds == null) ? 0 : rounds;
                long p = (reps   == null) ? 0 : reps;
                return base + (R_MAX - clamp(r, 0, R_MAX)) * 1_000_000L
                        + (REP_MAX - clamp(p, 0, REP_MAX)); // 높을수록 상위(역변환)
            }
            case "EMOM":
            case "SUCCESSFAIL": {
                int sf = isSuccess(nullToEmpty(result)) ? 0 : 1; // S 상위
                return base + sf;
            }
            case "EMOMMAX":
            case "MAXREPS": {
                long p = (reps == null) ? 0 : reps;
                return base + (REP_MAX - clamp(p, 0, REP_MAX)); // 높을수록 상위(역변환)
            }
            default:
                return base + LVL_STEP - 1; // 정의 안 됨 → 뒤로
        }
    }

    public String metricKey(String wodType, Integer rounds, Integer reps,
                            Float timeSeconds, String result) {
        String t = wodType == null ? "" : wodType.trim().toUpperCase();
        switch (t) {
            case "FORTIME":   return "t=" + ((timeSeconds == null) ? "NA" : Math.round(timeSeconds * 1000.0));
            case "AMRAP":     return "r=" + (rounds==null?0:rounds) + ",p=" + (reps==null?0:reps);
            case "EMOM":
            case "SUCCESSFAIL": return "sf=" + (result==null?"F":result.toUpperCase());
            case "EMOMMAX":
            case "MAXREPS":   return "p=" + (reps==null?0:reps);
            default:          return "NA";
        }
    }

    /**
     * 동점 해소용 멤버 문자열.
     * - ZSET은 score가 같을 때 "멤버 문자열"을 사전식으로 비교한다.
     * - 요구사항: 닉네임 오름차순 → (같으면) memberListPk → (그래도 같으면) recordPk
     * - 문자열 비교가 숫자 오름차순과 같아지도록 memberListPk는 0패딩 고정폭.
     */
    public String memberString(String level, String metricKey, String nickname,
                               Long recordPk, Long memberListPk) {
        String nick = normalizeNick(nickname);                 // 사전식 안정화
        String ml   = zeroPad(memberListPk==null?0L:memberListPk, 12); // "000000000045"
        return String.format("lvl=%d|met=%s|nick=%s|ml=%s|rid=%d",
                LevelRank.of(level), metricKey, nick, ml, recordPk);
    }

    // ===== helpers =====
    private static long clamp(long v, long min, long max){ return Math.max(min, Math.min(max, v)); }
    private static String nullToEmpty(String s){ return s==null?"":s; }
    private static boolean isSuccess(String s){
        String u = s.toUpperCase();
        return u.equals("S") || u.equals("SUCCESS") || u.equals("PASS"); // 안전 폭 넓힘
    }
    private static String normalizeNick(String s){ return nullToEmpty(s).trim().toLowerCase(); }
    private static String zeroPad(long v, int width) {
        String s = Long.toString(v);
        if (s.length() >= width) return s;
        StringBuilder sb = new StringBuilder(width);
        for (int i=0;i<width - s.length();i++) sb.append('0');
        return sb.append(s).toString();
    }
}