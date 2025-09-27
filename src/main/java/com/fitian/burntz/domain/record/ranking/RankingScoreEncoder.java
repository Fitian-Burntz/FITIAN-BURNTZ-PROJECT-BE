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
 * 운동 기록을 숫자와 문자열로 바꾸는 클래스
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

    // 기존 상수
    private static final long R_MAX   = 1_000_000L;
    private static final long REP_MAX = 1_000_000L;
    private static final long AMRAP_MULT = 1_000_000L; // rounds에 곱하는 배수

    /** 각 레벨 내에서 가능한 최대 offset (AMRAP 계산식의 최대값) */
    private static final long IN_LEVEL_MAX = R_MAX * AMRAP_MULT + REP_MAX; // 1_000_001_000_000

    /** 레벨 영역을 완전히 분리하기 위한 step: in-level 최대값보다 약간 큰 값으로 동적으로 설정 */
    private static final double LVL_STEP = (double)(IN_LEVEL_MAX + 10L); // 안전 마진

    /**
     * Public API: enum-based score 계산기 (완전 타입 안전)
     */
    public double scoreFor(WodType wodType, String level, Integer rounds, Integer reps,
                           Float timeSeconds, RecordResult result) {
        int lvl = LevelRank.of(level);
        double base = (double) lvl * LVL_STEP;

        // null wodType -> 뒤로 보냄
        if (wodType == null) {
            return base + LVL_STEP - 1;
        }

        switch (wodType) {
            case ForTime -> {
                // time null이면 in-level 최대값을 사용하여 같은 레벨 내 최하위로 보냄
                long ms = (timeSeconds == null) ? IN_LEVEL_MAX : Math.round(timeSeconds * 1000.0);
                return base + (double) ms;
            }
            case AMRAP -> {
                long r = (rounds == null) ? 0L : rounds;
                long p = (reps == null) ? 0L : reps;
                long clampedR = clamp(r, 0L, R_MAX);
                long clampedP = clamp(p, 0L, REP_MAX);
                double offset = (double) ( (R_MAX - clampedR) * AMRAP_MULT ) + (double)(REP_MAX - clampedP);
                return base + offset;
            }
            case EMOM, SuccessFail -> {
                boolean sf = isSuccess(result);
                int sfInt = sf ? 0 : 1;
                return base + (double) sfInt;
            }
            case EMOMMAX, MaxReps -> {
                long p = (reps == null) ? 0L : reps;
                long clampedP2 = clamp(p, 0L, REP_MAX);
                double offset = (double)(REP_MAX - clampedP2);
                return base + offset;
            }
            default -> {
                return base + LVL_STEP - 1; // 정의 안 됨 -> 뒤로
            }
        }
    }

    /**
     * Enum-based metricKey (사람/로그용)
     */
    public String metricKey(WodType wodType, Integer rounds, Integer reps,
                            Float timeSeconds, RecordResult result) {
        if (wodType == null) return "NA";
        switch (wodType) {
            case ForTime:
                return "t=" + ((timeSeconds == null) ? "NA" : Math.round(timeSeconds * 1000.0));
            case AMRAP:
                return "r=" + (rounds==null?0:rounds) + ",p=" + (reps==null?0:reps);
            case EMOM, SuccessFail:
                return "sf=" + (result==null?"F":result.name().toUpperCase());
            case EMOMMAX, MaxReps:
                return "p=" + (reps==null?0:reps);
            default:
                return "NA";
        }
    }

    /**
     * memberString unchanged logic (동점 해소용)
     */
    public String memberString(String level, String metricKey, String nickname,
                               Long recordPk, Long memberListPk) {
        String nick = normalizeNick(nickname);
        String ml   = zeroPad(memberListPk==null?0L:memberListPk, 12);
        return String.format("lvl=%d|met=%s|nick=%s|ml=%s|rid=%d",
                LevelRank.of(level), metricKey, nick, ml, recordPk);
    }

    // ===== helpers =====
    private static long clamp(long v, long min, long max){ return Math.max(min, Math.min(max, v)); }
    private static String nullToEmpty(String s){ return s==null?"":s; }
    private static boolean isSuccess(RecordResult r){
        if (r == null) return false;
        String u = r.name().toUpperCase();
        return u.equals("S") || u.equals("SUCCESS") || u.equals("PASS");
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