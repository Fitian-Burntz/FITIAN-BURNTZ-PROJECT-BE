package com.fitian.burntz.domain.record.service;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.service
 * @fileName : RankingService
 * @date : 2025-09-25
 * @description : 랭킹 서비스 (ZSET+파이프라인+30일 만료)
 */
import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.record.enums.RecordResult;
import com.fitian.burntz.domain.record.ranking.RankingScoreEncoder;
import com.fitian.burntz.domain.wod.enums.WodType;
import lombok.*;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;


/**
 * - 키: rk:{boxPk}:{yyyy-MM-dd}
 * - upsert/remove: 파이프라인으로 ZADD/ZREM + EXPIREAT(고정 만료)
 * - getRanking: Redis 우선 → 미스/장애 시 DB 폴백 + 재빌드(파이프라인)
 */
@Service
@RequiredArgsConstructor
public class RankingService {

    private final StringRedisTemplate redis;
    private final RankingScoreEncoder encoder;

    private String key(Long boxPk, LocalDate date){
        return String.format("rk:%d:%s", boxPk, date);
    }
    private Instant expireAt(LocalDate date) {
        // “그 날짜 + 30일 00:00” (조회만으로 TTL 연장 X)
        return date.plusDays(30).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
    }

    /** 생성/수정 반영 (업서트) */
    public void upsert(Record r) {
        String k = key(r.getWod().getBox().getBoxPk(), r.getWod().getWodDate());

        double score = encoder.scoreFor(
                r.getWod().getWodType(),   // enum 오버로드 사용
                r.getLevel(),
                r.getRound(), r.getReps(), r.getTime(), r.getResult()
        );

        String member = encoder.memberString(
                r.getLevel(),
                encoder.metricKey(r.getWod().getWodType(), r.getRound(), r.getReps(), r.getTime(), r.getResult()),
                nicknamePolicy(r),
                r.getRecordPk(),
                r.getMemberList()==null ? null : r.getMemberList().getMemberListPk()
        );

        Instant expAt = expireAt(r.getWod().getWodDate());

        redis.executePipelined((RedisCallback<Object>) con -> {
            con.zAdd(k.getBytes(), score, member.getBytes());  // ZADD
            con.keyCommands().expireAt(k.getBytes(), expAt);   // EXPIREAT(고정 만료)
            return null;
        });
    }

    /** 삭제 반영 — 스냅샷 기반 제거 지원 */
    public void remove(RankingSnapshot s) {
        String k = key(s.getBoxPk(), s.getDate());
        String member = encoder.memberString(
                s.getLevel(),
                encoder.metricKey(s.getWodType(), s.getRound(), s.getReps(), s.getTime(), s.getResult()),
                s.getNickname(),
                s.getRecordPk(),
                s.getMemberListPk()
        );
        redis.executePipelined((RedisCallback<Object>) con -> {
            con.zRem(k.getBytes(), member.getBytes());         // ZREM
            return null;
        });
    }

    /** 편의: Record 그대로 제거(변경 전/후 상태가 같을 때만 정확) */
    public void remove(Record r) {
        RankingSnapshot s = RankingSnapshot.fromRecord(r);
        remove(s);
    }

    /** 조회: Redis 우선 → 폴백(DB) → Redis 재빌드 */
    public List<RankingRow> getRanking(Long boxPk, LocalDate date, Supplier<List<RankingRow>> dbFallback) {
        String k = key(boxPk, date);
        try {
            Set<String> members = redis.opsForZSet().range(k, 0, -1);
            if (members != null && !members.isEmpty()) {
                List<RankingRow> out = new ArrayList<>(members.size());
                int rank = 1;
                for (String m : members) out.add(RankingRow.fromRankAndMember(rank++, m));
                return out;
            }
        } catch (DataAccessException ignored) { /* Redis 일시 장애 → 폴백 */ }

        // 폴백: DB 정렬 → Redis 재빌드
        List<RankingRow> rebuilt = dbFallback.get();
        if (!rebuilt.isEmpty()) {
            Instant expAt = expireAt(date);
            redis.executePipelined((RedisCallback<Object>) con -> {
                byte[] kb = k.getBytes();
                for (RankingRow e : rebuilt) con.zAdd(kb, e.score, e.member.getBytes());
                con.keyCommands().expireAt(kb, expAt);
                return null;
            });
        }
        return rebuilt;
    }

    private String nicknamePolicy(Record r) {
        return (r.getNickname()!=null ? r.getNickname()
                : (r.getMemberList()!=null ? r.getMemberList().getBoxNickname() : ""));
    }

    // ===== 조회/적재 공용 Row =====
    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class RankingRow {
        private int rank;
        private Long recordPk;
        private Long memberListPk; // 게스트 null
        private String nickname;   // 소문자 정규화(lex)
        private String level;      // "0/1/2/3" or 원문
        private String metric;     // "t=..", "r=..,p=..", "sf=..", "p=.."

        // Redis 적재용 내부 필드
        private transient double score;
        private transient String member;

        /** "lvl=..|met=..|nick=..|ml=000..|rid=.." → 파싱 */
        public static RankingRow fromRankAndMember(int rank, String s){
            String[] parts = s.split("\\|");
            String lvl  = parts[0].substring("lvl=".length());
            String met  = parts[1].substring("met=".length());        // ← FIX
            String nick = parts[2].substring("nick=".length());
            String ml   = parts[3].substring("ml=".length());
            Long rid    = Long.valueOf(parts[4].substring("rid=".length()));

            RankingRow e = new RankingRow();
            e.rank = rank;
            e.recordPk = rid;
            e.memberListPk = "000000000000".equals(ml) ? null : Long.valueOf(ml);
            e.nickname = nick;
            e.level = lvl;
            e.metric = met;
            e.member = s;
            return e;
        }
    }

    /** 랭킹 제거용 스냅샷 (업데이트 전 상태 보존) */
    @Getter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class RankingSnapshot {
        private Long boxPk;
        private LocalDate date;
        private WodType wodType;      // enum
        private String level;
        private Integer round;
        private Integer reps;
        private Float time;
        private RecordResult result;   // enum
        private String nickname;       // 정책 적용 후 값
        private Long memberListPk;     // null 허용
        private Long recordPk;

        public static RankingSnapshot fromRecord(Record r) {
            return RankingSnapshot.builder()
                    .boxPk(r.getWod().getBox().getBoxPk())
                    .date(r.getWod().getWodDate())
                    .wodType(r.getWod().getWodType())
                    .level(r.getLevel())
                    .round(r.getRound())
                    .reps(r.getReps())
                    .time(r.getTime())
                    .result(r.getResult())
                    .nickname(r.getNickname()!=null ? r.getNickname()
                            : (r.getMemberList()!=null ? r.getMemberList().getBoxNickname() : ""))
                    .memberListPk(r.getMemberList()==null? null : r.getMemberList().getMemberListPk())
                    .recordPk(r.getRecordPk())
                    .build();
        }
    }
}