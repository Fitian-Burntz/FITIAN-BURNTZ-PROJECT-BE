package com.fitian.burntz.domain.record.service;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.service
 * @fileName : RankingService
 * @date : 2025-09-25
 * @description : 랭킹 서비스 (ZSET+파이프라인+30일 만료)
 */
import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.record.ranking.RankingScoreEncoder;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;


@Service
@RequiredArgsConstructor
@Slf4j
public class RankingService {

    private final StringRedisTemplate redis;
    private final RankingScoreEncoder encoder;

    /**
     * Redis 키 생성(rk:{boxPk}:{date}) 
     */
    private String key(Long boxPk, LocalDate date) {
        return String.format("rk:%d:%s", boxPk, date);
    }

    /**
     * 만료 시각 계산 (30일 후)
     * 입력 시간 + 30일 후 한국시간으로 자정에 redis에서 삭제 -> 그 후 조회시 DB에서 조회
     */
    private Instant expireAt(LocalDate date) {
        return date.plusDays(30).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();
    }

    /**
     * 생성/수정 (upsert)
     */
    public void upsert(Record r) {
        try {
            //1. 키 생성
            String k = key(r.getWod().getBox().getBoxPk(), r.getWod().getWodDate());

            //2. 점수 계산
            double score = encoder.scoreFor(
                    r.getWod().getWodType(),
                    r.getLevel(),
                    r.getRound(),
                    r.getReps(),
                    r.getTime(),
                    r.getResult()
            );

            //3. 멤버 문자열 생성
            String member = encoder.memberString(
                    r.getLevel(),
                    getNickname(r),
                    r.getRecordPk(),
                    getMemberListPk(r)
            );

            //4. 만료시각 계산
            Instant expAt = expireAt(r.getWod().getWodDate());

            //5. 파이프라인으로 Redis 명령 실행
            redis.executePipelined((RedisCallback<Object>) con -> {
                con.zSetCommands().zAdd(k.getBytes(), score, member.getBytes());
                con.keyCommands().expireAt(k.getBytes(), expAt);
                return null;
            });
        } catch (Exception e) {
            log.warn("Redis upsert failed for recordPk={}: {}", r.getRecordPk(), e.toString());
        }
    }

    /**
     * 삭제
     */
    public void remove(Long boxPk, LocalDate date, String level, String nickname,
                       Long recordPk, Long memberListPk) {
        try {
            String k = key(boxPk, date);
            String member = encoder.memberString(level, nickname, recordPk, memberListPk);

            redis.executePipelined((RedisCallback<Object>) con -> {
                con.zRem(k.getBytes(), member.getBytes());
                return null;
            });
        } catch (Exception e) {
            log.warn("Redis remove failed for recordPk={}: {}", recordPk, e.toString());
        }
    }

    /**
     * 조회 (Redis 우선 → DB 폴백)
     */
    public List<Long> getRanking(Long boxPk, LocalDate date, Supplier<List<Long>> dbFallback) {
        String k = key(boxPk, date);
        
        // 1. Redis 시도
        try {
            Set<String> members = redis.opsForZSet().range(k, 0, -1);
            if (members != null && !members.isEmpty()) {
                return members.stream()
                        .map(encoder::extractRecordPk)
                        .toList();
            }
        } catch (DataAccessException e) {
            log.warn("Redis access failed, fallback to DB: {}", e.toString());
        }

        // 2. Redis 조회 실패 시 DB 폴백
        log.info("Redis MISS - fallback to DB"); // ← 추가
        return dbFallback.get();
    }

    /**
     * Redis 재빌드 (DB → Redis)
     * Redis Miss 발생시 자동 호출되어 DB에서 가져온 정렬 데이터를 Redis에 다시 저장
     */
    public void rebuild(Long boxPk, LocalDate date, List<Record> sortedRecords) {
        if (sortedRecords.isEmpty()) return;

        try {
            String k = key(boxPk, date);
            Instant expAt = expireAt(date);

            redis.executePipelined((RedisCallback<Object>) con -> {
                byte[] kb = k.getBytes();

                for (Record r : sortedRecords) {
                    double score = encoder.scoreFor(
                            r.getWod().getWodType(),
                            r.getLevel(),
                            r.getRound(),
                            r.getReps(),
                            r.getTime(),
                            r.getResult()
                    );

                    String member = encoder.memberString(
                            r.getLevel(),
                            getNickname(r),
                            r.getRecordPk(),
                            getMemberListPk(r)
                    );

                    con.zAdd(kb, score, member.getBytes());
                }

                con.keyCommands().expireAt(kb, expAt);
                return null;
            });
        } catch (Exception e) {
            log.warn("Redis rebuild failed for box={}, date={}: {}", boxPk, date, e.toString());
        }
    }

    /**
     * 닉네임 변경 시 여러 Record를 한 번에 업데이트 (배치 처리)
     * 같은 Redis 키에 대해 파이프라인으로 한 번에 처리하여 성능 최적화
     */
    public void updateNicknamesBatch(List<Record> records, String oldNickname, Long memberListPk) {
        if (records.isEmpty()) return;

        try {
            // 같은 키를 사용하므로 첫 번째 레코드에서 추출
            Record first = records.get(0);
            String k = key(first.getWod().getBox().getBoxPk(), first.getWod().getWodDate());
            Instant expAt = expireAt(first.getWod().getWodDate());

            // 파이프라인으로 한 번에 처리 (remove + add)
            redis.executePipelined((RedisCallback<Object>) con -> {
                byte[] kb = k.getBytes();

                for (Record r : records) {
                    // 1. 기존 항목 삭제 (old nickname)
                    String oldMember = encoder.memberString(
                            r.getLevel(),
                            oldNickname,
                            r.getRecordPk(),
                            memberListPk
                    );
                    con.zSetCommands().zRem(kb, oldMember.getBytes());

                    // 2. 새 항목 추가 (new nickname)
                    double score = encoder.scoreFor(
                            r.getWod().getWodType(),
                            r.getLevel(),
                            r.getRound(),
                            r.getReps(),
                            r.getTime(),
                            r.getResult()
                    );

                    String newMember = encoder.memberString(
                            r.getLevel(),
                            getNickname(r),
                            r.getRecordPk(),
                            getMemberListPk(r)
                    );

                    con.zSetCommands().zAdd(kb, score, newMember.getBytes());
                }

                // 만료 시간 갱신
                con.keyCommands().expireAt(kb, expAt);
                return null;
            });

            log.info("Batch updated {} records for key: {}", records.size(), k);

        } catch (Exception e) {
            log.warn("Redis batch update failed: {}", e.toString());
        }
    }


    /**
     * 닉네임 추출 헬퍼
     */
    private String getNickname(Record r) {
        return r.getNickname() != null ? r.getNickname()
                : (r.getMemberList() != null ? r.getMemberList().getBoxNickname() : "");
    }

    /**
     * memberListPk 추출 헬퍼
     */
    private Long getMemberListPk(Record r) {
        return r.getMemberList() != null ? r.getMemberList().getMemberListPk() : null;
    }
}