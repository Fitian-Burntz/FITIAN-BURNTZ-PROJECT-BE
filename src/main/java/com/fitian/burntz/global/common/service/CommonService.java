package com.fitian.burntz.global.common.service;

import com.fitian.burntz.global.common.entity.Agreement;
import com.fitian.burntz.global.common.repository.AgreementRepository;
import com.fitian.burntz.global.common.v1.dto.AgreementCreateRequestDto;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.common.service
 * @fileName : AgreementService
 * @date : 2026-01-29
 * @description : 이용 약관 서비스 단입니다.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class CommonService {

    private final AgreementRepository agreementRepository;

    public void createAgreement(AgreementCreateRequestDto dto) {
        Agreement agreement = Agreement.builder()
                .title(dto.getTitle())
                .language(dto.getLanguage())
                .content(dto.getContent())
                .build();

        agreementRepository.save(agreement);
    }

    public Agreement getAgreement(Long agreementPk) {
        return agreementRepository.findById(agreementPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.AGREEMENT_NOT_FOUND));
    }
}
