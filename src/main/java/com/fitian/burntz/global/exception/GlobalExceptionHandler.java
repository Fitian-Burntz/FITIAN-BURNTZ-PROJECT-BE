package com.fitian.burntz.global.exception;


import com.fitian.burntz.global.common.response.ResponseDTO;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ResponseDTO> handleValidationException(ValidationException e, HttpServletRequest request) {
        log.warn("[{}] {} | {} | member={}", e.getErrorCode().getCode(), e.getMessage(), buildContext(request, e.getDetail()), currentMemberPk());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ResponseDTO.of(e.getErrorCode()));
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<ResponseDTO> handleInternalServerException(InternalServerException e, HttpServletRequest request) {
        log.error("[{}] {} | {} | member={}", e.getErrorCode().getCode(), e.getMessage(), buildContext(request, e.getDetail()), currentMemberPk());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ResponseDTO.of(e.getErrorCode()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ResponseDTO> handleNotFoundException(NotFoundException e, HttpServletRequest request) {
        log.warn("[{}] {} | {} | member={}", e.getErrorCode().getCode(), e.getMessage(), buildContext(request, e.getDetail()), currentMemberPk());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus())
                .body(ResponseDTO.of(e.getErrorCode()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDTO> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("[ILLEGAL_ARGUMENT] {} | {} | member={}", e.getMessage(), buildContext(request, null), currentMemberPk());
        return ResponseEntity.badRequest()
                .body(ResponseDTO.of(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[INVALID_INPUT] {} | member={}", message, currentMemberPk());
        return ResponseEntity.badRequest()
                .body(ResponseDTO.builder()
                        .localDateTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .code(ErrorCode.INVALID_INPUT_VALUE.getCode())
                        .message(message)
                        .build());
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseDTO.of(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO> handleException(Exception e, HttpServletRequest request) {
        log.error("[UNHANDLED] {} | {} | member={}", e.getMessage(), buildContext(request, null), currentMemberPk(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseDTO.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @SuppressWarnings("unchecked")
    private String buildContext(HttpServletRequest request, String detail) {
        List<String> parts = new ArrayList<>();

        Map<String, String> pathVars = (Map<String, String>)
                request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVars != null && !pathVars.isEmpty()) {
            parts.add("pathVars=" + pathVars);
        }

        if (request.getQueryString() != null) {
            parts.add("query=" + request.getQueryString());
        }

        if (detail != null) {
            parts.add("detail=" + detail);
        }

        return parts.isEmpty() ? "-" : String.join(" ", parts);
    }

    private String currentMemberPk() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails ud) {
            return String.valueOf(ud.getMemberPk());
        }
        return MDC.get("memberPk") != null ? MDC.get("memberPk") : "anonymous";
    }

}
