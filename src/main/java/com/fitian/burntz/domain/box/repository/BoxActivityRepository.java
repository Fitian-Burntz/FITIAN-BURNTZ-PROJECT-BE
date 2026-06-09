package com.fitian.burntz.domain.box.repository;

import com.fitian.burntz.domain.box.entity.BoxActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoxActivityRepository extends JpaRepository<BoxActivity, Long> {
    Page<BoxActivity> findByBoxPkOrderByCreatedAtDesc(Long boxPk, Pageable pageable);
}
