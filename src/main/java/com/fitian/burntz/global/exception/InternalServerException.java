package com.fitian.burntz.global.exception;

public class InternalServerException extends CustomException {

  public InternalServerException(ErrorCode errorCode) {
    super(errorCode);
  }

  public InternalServerException(ErrorCode errorCode, String detail) {
    super(errorCode, detail);
  }
}
