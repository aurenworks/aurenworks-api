package com.aurenworks.api;

import java.util.Map;

public record ErrorEnvelope(ErrorInfo error) {
  public record ErrorInfo(String code, String message, Map<String, Object> details, String requestId) {
  }

  public static ErrorEnvelope of(String code, String message, Map<String, Object> details, String requestId) {
    return new ErrorEnvelope(new ErrorInfo(code, message, details, requestId));
  }
}
