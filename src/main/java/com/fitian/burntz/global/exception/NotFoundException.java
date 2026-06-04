package com.fitian.burntz.global.exception;

public class NotFoundException extends CustomException {

  public NotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }

  public NotFoundException(ErrorCode errorCode, String detail) {
    super(errorCode, detail);
  }
}
