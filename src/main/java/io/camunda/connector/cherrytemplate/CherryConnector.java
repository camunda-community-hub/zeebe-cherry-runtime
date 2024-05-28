package io.camunda.connector.cherrytemplate;


/* ******************************************************************** */
/*                                                                      */
/*  CherryConnector                                                     */
/*                                                                      */
/*  This interface is not required by Cherry, but it is the method      */
/*  searched by the framework to collect additional information on      */
/*  the connector                                                       */
/*                                                                      */
/*  Note: the class must use the @OutboundConnector annotation, so      */
/*  name and type is available on this annotation.                      */
/*  Input variable may be available in the annotation, but it's not     */
/*  enough for Cherry, which needs more information (dropdown?          */
/*  Condition?                                                          */
/*                                                                      */
/*  Example:                                                            */
/*  @OutboundConnector(name = "PdfFunction",                             */
/*         inputVariables = { PdfInput.INPUT_SOURCE_FILE},              */
/*         type = "c-pdf-function")                                     */
/*                                                                      */
/* public class MyFunction implements OutboundConnectorFunction, CherryConnector */
/* ******************************************************************** */

import java.util.List;
import java.util.Map;

public interface CherryConnector {

  /**
   * return a description of the connector
   *
   * @return the description
   */
  String getDescription();

  /**
   * Return the logo
   *
   * @return the log (AVG string)
   */
  String getLogo();

  /**
   * Return the collection name of the connector
   *
   * @return the collection name
   */
  String getCollectionName();

  /*
  return a list of BPMN Errors
  Map of BPMN_ERROR_CODE, Explanation
   */
  Map<String, String> getListBpmnErrors();

  /**
   * return a CherryOutput class to describe the Input
   *
   * @return a CherryInput class
   */
  Class<?> getInputParameterClass();

  /**
   * return a CherryOutput class to describe the output
   *
   * @return a CherryOutput class
   */
  Class<?> getOutputParameterClass();

  /**
   * on which BPMN item this event can apply. Return a list like
   * "bpmn:Task",
   * "bpmn:IntermediateThrowEvent",
   * "bpmn:IntermediateCatchEvent"
   *
   * @return list of bpmn item
   */
  List<String> getAppliesTo();
}
