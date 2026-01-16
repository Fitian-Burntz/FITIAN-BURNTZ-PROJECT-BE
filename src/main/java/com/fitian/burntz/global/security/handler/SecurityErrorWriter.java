package com.fitian.burntz.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitian.burntz.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.security.handler
 * @fileName : SecurityErrorWriter
 * @date : 2026-01-16
 * @description : ErrorCode를 받아오는 클래스입니다.
 */
public class SecurityErrorWriter {

    private SecurityErrorWriter() {}

    public static void write(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", errorCode.getCode());
        body.put("message", errorCode.getMessage());
        body.put("status", errorCode.getHttpStatus().value());
        body.put("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getWriter(), body);
    }
}
