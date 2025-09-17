package com.fitian.burntz.domain.box.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.box.repository
 * @fileName : BoxRepository
 * @date : 2025-09-08
 * @description : 박스 리포지토리입니다.
 */
public interface BoxRepository extends JpaRepository<Box, Long> {
    Optional<Box> findByBoxCode(String boxCode);
    //모든 박스 불러오는 메서드(삭제된 박스 제외)
    Optional<Box> findByBoxPkAndDeletedYN(Long boxPk, BaseTime.Yn deletedYN);
}
