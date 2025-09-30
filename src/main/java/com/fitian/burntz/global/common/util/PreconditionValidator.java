package com.fitian.burntz.global.common.util;

import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import static com.fitian.burntz.global.common.util.StringUtil.trimToNull;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.global.common.util
 * @fileName : ControllerValidationHelper
 * @date : 2025-09-26
 * @description : 주로 파라미터 null 값 체크 용으로 사용되는 검증 헬퍼 (null 체크 중복 제거)
 */

@Component
public class PreconditionValidator {

    /**
     * 인증된 사용자의 memberPk를 반환합니다.
     * 인증되지 않았으면 ValidationException(ErrorCode.UNAUTHORIZED) 발생.
     */
    public Long requireLogin(CustomUserDetails customUserDetails) {
        Long loginMemberPk = customUserDetails == null ? null : customUserDetails.getMemberPk();

        if (loginMemberPk == null) {
            throw new ValidationException(ErrorCode.UNAUTHORIZED);
        }

        return loginMemberPk;
    }

    public String requireDeviceId(String deviceId) {
        String trimDeviceId = trimToNull(deviceId);

        if (trimDeviceId == null) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        return trimDeviceId;
    }

    /** 인증과 관계 없이 memberPk 값이 잘 넘어왔는지 검증 **/
    public Long requireMemberPk(Long memberPk) {
        if (memberPk == null) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        return memberPk;
    }

    /** boxPk 값이 잘 넘어왔는지 검증 **/
    public Long requireBoxPk(Long boxPk) {
        if (boxPk == null) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        return boxPk;
    }

    /**
     * boxCode를 trim 후 null일 경우 ValidationException(ErrorCode.MISSING_REQUIRED_FIELD) 발생.
     * 정상일 경우 정리된 문자열 반환.
     */
    public String requireBoxCode(String boxCode) {
        String trimBoxCode = trimToNull(boxCode);

        if (trimBoxCode == null) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        return trimBoxCode;
    }

    /** String 값을 받을 경우 값 정제 후 null 체크
     * boxCode 검증 로직과 동일하지만 boxCode 는 중요 값이므로
     * 명시적 검증을 확인했다는 것을 알아챌 수 있게 함.**/
    public String requiredStringValue(String value){
        String trimValue = trimToNull(value);

        if (trimValue == null) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        return trimValue;
    }

    /**
     * Pageable의 pageSize가 클 경우 maxSize로 제한한 PageRequest를 반환.
     * 기존 정렬(sort) 정보는 유지됩니다.
     */
    public Pageable limitPageable(Pageable pageable, int maxSize) {
        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), maxSize),
                pageable.getSort()
        );
    }
}
