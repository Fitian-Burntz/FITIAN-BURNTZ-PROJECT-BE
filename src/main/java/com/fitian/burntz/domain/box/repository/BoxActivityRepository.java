package com.fitian.burntz.domain.box.repository;

import com.fitian.burntz.domain.box.entity.BoxActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoxActivityRepository extends JpaRepository<BoxActivity, Long> {
    Page<BoxActivity> findByBoxPkOrderByCreatedAtDesc(Long boxPk, Pageable pageable);

    @Modifying
    @Query("DELETE FROM BoxActivity a WHERE a.boxPk = :boxPk")
    void deleteAllByBoxPk(@Param("boxPk") Long boxPk);
}
