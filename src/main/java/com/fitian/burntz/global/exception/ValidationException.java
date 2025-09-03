package com.fitian.burntz.global.exception;

public class ValidationException extends CustomException {

  public ValidationException(ErrorCode errorCode) {
    super(errorCode);
  }
}
