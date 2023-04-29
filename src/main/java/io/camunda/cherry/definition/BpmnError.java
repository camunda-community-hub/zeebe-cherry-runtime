/* ******************************************************************** */
/*                                                                      */
/*  BpmnErrorParameter                                                  */
/*                                                                      */
/*  Declare a BPMN error that the executor can throw                    */
/* ******************************************************************** */
package io.camunda.cherry.definition;

public class BpmnError {

  /**
   * Describe a BPMNError : code and explanation
   */
  private final String code;
  private final String explanation;

  public BpmnError(String code, String explanation) {
    this.code = code;
    this.explanation = explanation;
  }

  /**
   * Create a Bpmn ControllerPage explanation
   *
   * @param code        Code of error
   * @param explanation Explanation for this code
   * @return
   */
  public static BpmnError getInstance(String code, String explanation) {
    return new BpmnError(code, explanation);
  }

  public String getCode() {
    return code;
  }

  public String getExplanation() {
    return explanation;
  }

}


