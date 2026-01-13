package com.fitian.burntz.domain.alarm.service;

import com.fitian.burntz.domain.alarm.entity.FcmToken;
import com.fitian.burntz.domain.alarm.repository.FcmTokenRepository;
import com.fitian.burntz.domain.alarm.v1.dto.FcmTokenCreateRequest;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.service
 * @fileName : AlarmService
 * @date : 2026-01-12
 * @description : 알람 서비스 입니다.
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlarmService {

    private FcmTokenRepository fcmTokenRepository;
    private MemberRepository memberRepository;

    public FcmToken upsertToken(CustomUserDetails userDetails, FcmTokenCreateRequest request) {
        return fcmTokenRepository.findTokenByMemberMemberPkAndDeviceIdAndDeletedYN(userDetails.getMemberPk(), request.getDeviceId())
                .orElseGet(() -> {
                    Member member = memberRepository.findById(userDetails.getMemberPk())
                            .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
                    FcmToken newToken = FcmToken.builder()
                            .deviceId(request.getDeviceId())
                            .token(request.getToken())
                            .isActive("1")
                            .member(member)
                            .build();
                    return fcmTokenRepository.save(newToken);
                });
    }
}
