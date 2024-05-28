package io.camunda.cherry.exception;

public class OperationException extends Exception {
  private final String exceptionCode;
  private final String explanation;

  public OperationException(String exceptionCode, String explanation) {
    this.exceptionCode = exceptionCode;
    this.explanation = explanation;
  }

  public String getExceptionCode() {
    return exceptionCode;
  }

  public String getExplanation() {
    return explanation;
  }

  public String getHumanInformation() {
    return "Code: [" + exceptionCode + "] " + explanation;
  }
}
