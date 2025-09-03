package com.fitian.burntz.global.common.response;

import com.fitian.burntz.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "공통 응답 DTO")

public class ResponseDTO<T> {

  private LocalDateTime localDateTime;
  private int statusCode;
  private String code;
  private String message;

  T data;

  public static <T> ResponseDTO<T> of(ErrorCode errorCode, T data) {
    return ResponseDTO.<T>builder()
        .localDateTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
        .code(errorCode.getCode())
        .statusCode(errorCode.getHttpStatus().value())
        .data(data)
        .message(errorCode.getMessage())
        .build();
  }

  public static <T> ResponseDTO<T> of(ErrorCode errorCode) {
    return ResponseDTO.<T>builder()
        .localDateTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
        .code(errorCode.getCode())
        .statusCode(errorCode.getHttpStatus().value())
        .message(errorCode.getMessage())
        .data(null)
        .build();
  }
  
  public static <T> ResponseDTO<T> of(T data, String message) {
    return ResponseDTO.<T>builder()
        .localDateTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
        .code("SUCCESS")
        .statusCode(200)
        .message(message)
        .data(data)
        .build();
  }

}
