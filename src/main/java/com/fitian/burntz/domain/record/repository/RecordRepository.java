package com.fitian.burntz.domain.record.repository;

import com.fitian.burntz.domain.record.entity.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.repository
 * @fileName : RecordRepository
 * @date : 2025-09-17
 * @description : Record Repository
 */

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
}
