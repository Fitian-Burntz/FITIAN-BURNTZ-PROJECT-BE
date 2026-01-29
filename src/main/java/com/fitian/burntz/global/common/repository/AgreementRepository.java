package com.fitian.burntz.global.common.repository;

import com.fitian.burntz.global.common.entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.common.repository
 * @fileName : AgreementRepository
 * @date : 2026-01-29
 * @description : Agreement 리포지토리
 */
public interface AgreementRepository extends JpaRepository<Agreement, Long> {

    Optional<Agreement> findByLanguageAndTitle(String language, String title);
}
