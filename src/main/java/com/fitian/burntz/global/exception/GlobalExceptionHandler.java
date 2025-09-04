package com.fitian.burntz.global.exception;


import com.fitian.burntz.global.common.response.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ResponseDTO> providerNotFoundException(ValidationException e) {
    return ResponseEntity.status(e.getErrorCode().getHttpStatus())
        .body(ResponseDTO.of(e.getErrorCode()));
  }

  @ExceptionHandler(InternalServerException.class)
  public ResponseEntity<ResponseDTO> internalServerException(InternalServerException e) {
    return ResponseEntity.status(e.getErrorCode().getHttpStatus())
        .body(ResponseDTO.of(e.getErrorCode()));
  }

}
