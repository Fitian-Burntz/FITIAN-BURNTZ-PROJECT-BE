package com.fitian.burntz.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitian.burntz.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.security.handler
 * @fileName : RestAccessDeniedHandler
 * @date : 2026-01-16
 * @description : 진입 거부 핸들러 입니다.
 */

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        SecurityErrorWriter.write(response, objectMapper, ErrorCode.ACCESS_DENIED);
    }
}
