/* ******************************************************************** */
/*                                                                      */
/*  CherryInput                                                         */
/*                                                                      */
/*  Use this interface to implement the Input parameter.                */
/* then the Cherry template will be able to read the Input information  */
/* ******************************************************************** */
package io.camunda.connector.cherrytemplate;

import java.util.List;
import java.util.Map;

public interface CherryInput {

  /**
   * These constant map PARAMETER_NAME_xxx constant in SDKRunnerCherryConnector
   */

  String PARAMETER_MAP_NAME = "name";
  String PARAMETER_MAP_LABEL = "label";
  String PARAMETER_MAP_CLASS = "class";
  String PARAMETER_MAP_LEVEL = "level";
  String PARAMETER_MAP_LEVEL_REQUIRED = "REQUIRED";
  String PARAMETER_MAP_LEVEL_OPTIONAL = "OPTIONAL";
  String PARAMETER_MAP_EXPLANATION = "explanation";
  String PARAMETER_MAP_DEFAULT_VALUE = "defaultValue";
  String PARAMETER_MAP_GSON_TEMPLATE = "gsonTemplate";

  String PARAMETER_MAP_CONDITION = "condition";
  String PARAMETER_MAP_CONDITION_EQUALS = "conditionEquals";
  String PARAMETER_MAP_CONDITION_ONE_OF = "conditionOneOf";
  String PARAMETER_MAP_CHOICE_LIST = "choiceList";
  String PARAMETER_MAP_CHOICE_LIST_CODE = "code";
  String PARAMETER_MAP_CHOICE_LIST_DISPLAY_NAME = "displayName";
  String PARAMETER_MAP_VISIBLE_IN_TEMPLATE = "visibleInTemplate";

  /**
   * get the list of Input Parameters
   *
   * @return list of Map. Map contains key "name", "label", "defaultValue", "class", "level", "explanation"
   */
  List<Map<String, Object>> getInputParameters();

}
