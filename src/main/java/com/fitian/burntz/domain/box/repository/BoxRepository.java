package com.fitian.burntz.domain.box.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 삭제되지 않은 boxPk 조회
    @Query("SELECT b FROM Box b WHERE b.boxPk = :boxPk AND b.deletedYN = 'N'")
    Optional<Box> findActiveById(@Param("boxPk") Long boxPk);

    // 삭제되지 않은 박스 코드만 가져오기
    @Query("SELECT b FROM Box b WHERE b.boxCode = :boxCode AND b.deletedYN = 'N'")
    Optional<Box> findActiveByBoxCode(@Param("boxCode") String boxCode);

    //모든 박스 불러오는 메서드(삭제된 박스 제외)
    Optional<Box> findByBoxPkAndDeletedYN(Long boxPk, BaseTime.Yn deletedYN);

    // 삭제되지 않은 박스 전체 리스트 페이징 해서 조회
    Page<Box> findAllByDeletedYN(BaseTime.Yn deletedYn, Pageable pageable);

    @Query("select case when (count(b) > 0) then true else false end " +
            "from Box b where b.boxCode = :boxCode and b.deletedYN = 'N'")
    boolean existsActiveByBoxCode(@Param("boxCode") String boxCode);



}
