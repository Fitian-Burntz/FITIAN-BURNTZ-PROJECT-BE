package com.fitian.burntz.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitian.burntz.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.security.handler
 * @fileName : RestAuthenticationEntryPoint
 * @date : 2026-01-16
 * @description : 인증 진입포인트를 컨트롤하는 클래스입니다.
 */

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String ATTR_ERROR_CODE = "AUTH_ERROR_CODE";

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ErrorCode errorCode = (ErrorCode) request.getAttribute(ATTR_ERROR_CODE);

        if(errorCode == null) {
            String auth = request.getHeader("Authorization");
            if(!StringUtils.hasText(auth)) {
                errorCode = ErrorCode.TOKEN_MISSING;
            } else if (!auth.startsWith("Bearer ")) {
                errorCode = ErrorCode.TOKEN_FORMAT_INVALID;
            } else {
                errorCode = ErrorCode.UNAUTHORIZED;
            }
        }
        SecurityErrorWriter.write(response, objectMapper, errorCode);
    }
}
