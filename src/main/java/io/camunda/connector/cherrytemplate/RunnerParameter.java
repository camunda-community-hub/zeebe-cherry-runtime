/* ******************************************************************** */
/*                                                                      */
/*  WorkerParameter                                                     */
/*                                                                      */
/*  Define a parameters (Worker or Connector)                           */
/* ******************************************************************** */
package io.camunda.connector.cherrytemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunnerParameter {
  public static final String ACCESS_ALL_VARIABLES = "*";
  static Logger logger = LoggerFactory.getLogger(RunnerParameter.class.getName());
  /**
   * class to declare a parameter
   */
  public String name;

  public String label;
  public Class<?> clazz;
  public Object defaultValue;
  public Level level;
  public String explanation;
  public String gsonTemplate;
  public String feelOptional = "optional";
  /**
   * Declare a condition on the parameters
   */
  public String condition;
  public String conditionEquals;

  public List<String> conditionOneOf;
  public List<WorkerParameterChoice> choiceList;
  public boolean visibleInTemplate = false;
  public Group group;

  public RunnerParameter() {

  }

  public RunnerParameter(String name, String label, Class<?> classParameter, Level level, String explanation) {
    this.name = name;
    this.label = label;
    this.clazz = classParameter;
    this.level = level;
    this.explanation = explanation;
  }

  /**
   * Get an instance without a default value
   *
   * @param parameterName parameter name
   * @param clazz         class of the expected parameter
   * @param defaultValue  the default value for this parameter, if no value is given. Note: a
   *                      required parameter may have a null as a value.
   * @param level         level for this parameter
   * @param explanation   describe the usage of the parameter
   * @return a WorkerParameter
   */
  public static RunnerParameter getInstance(String parameterName,
                                            String parameterLabel,
                                            Class<?> clazz,
                                            Object defaultValue,
                                            Level level,
                                            String explanation) {
    RunnerParameter parameter = new RunnerParameter();
    parameter.name = parameterName;
    parameter.label = parameterLabel;
    parameter.clazz = clazz;
    parameter.defaultValue = defaultValue;
    parameter.level = level;
    parameter.explanation = explanation;
    return parameter;
  }

  /* -------------------------------------------------------- */
  /*                                                          */
  /*  main information                                        */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * Get an instance without a default value
   *
   * @param parameterName  parameter name
   * @param parameterLabel label to display in the template
   * @param clazz          class of the expected parameter
   * @param level          level for this parameter
   * @param explanation    describe the usage of the parameter
   * @return a WorkerParameter
   */
  public static RunnerParameter getInstance(String parameterName,
                                            String parameterLabel,
                                            Class<?> clazz,
                                            Level level,
                                            String explanation) {
    RunnerParameter parameter = new RunnerParameter();
    parameter.name = parameterName;
    parameter.label = parameterLabel;
    parameter.clazz = clazz;
    parameter.defaultValue = null;
    parameter.level = level;
    parameter.explanation = explanation;
    return parameter;
  }

  /**
   * A Gson Parameter can be given as a String or as Object?
   *
   * @param parameterName  parameter name
   * @param parameterLabel label to display in the template
   * @param level          level for this parameter
   * @param explanation    describe the usage of the parameter
   * @param gsonTemplate   Give an example of the format, to verify that the parameters meet the
   *                       requirement. Format is <Parameter>:<Class>/Example {"host":"String", "Port": "Integer"}
   * @return a runnerParameter from parameters
   */
  public static RunnerParameter getGsonInstance(String parameterName,
                                                String parameterLabel,
                                                Level level,
                                                String explanation,
                                                String gsonTemplate) {
    RunnerParameter parameter = new RunnerParameter();
    parameter.name = parameterName;
    parameter.label = parameterLabel;
    parameter.clazz = Object.class;
    parameter.defaultValue = null;
    parameter.level = level;
    parameter.explanation = explanation;
    parameter.gsonTemplate = gsonTemplate;
    return parameter;
  }

  /**
   * The worker/connector wants to access all variables
   *
   * @param explanation why do you need this access?
   * @return runnerParameter which can access all variables
   */
  public static RunnerParameter getAccessAllVariables(String explanation) {
    RunnerParameter parameter = new RunnerParameter();
    parameter.name = ACCESS_ALL_VARIABLES;
    parameter.label = "Access All Variables";
    parameter.clazz = String.class;
    parameter.defaultValue = null;
    parameter.level = Level.OPTIONAL;
    parameter.explanation = explanation;
    return parameter;
  }

  public static RunnerParameter fromMap(Map<String, Object> inputMap, String contextInfo) {
    RunnerParameter parameter = new RunnerParameter();
    parameter.name = getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_NAME, contextInfo);
    parameter.label = getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_LABEL, contextInfo);
    parameter.clazz = (Class) inputMap.get(CherryInput.PARAMETER_MAP_CLASS);
    parameter.level = Level.valueOf(getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_LEVEL, contextInfo));
    parameter.explanation = getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_EXPLANATION, contextInfo);
    parameter.defaultValue = inputMap.get(CherryInput.PARAMETER_MAP_DEFAULT_VALUE);

    parameter.gsonTemplate = getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_GSON_TEMPLATE, contextInfo);

    parameter.condition = getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_CONDITION, contextInfo);
    parameter.conditionEquals = getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_CONDITION_EQUALS, contextInfo);
    parameter.conditionOneOf = (List<String>) inputMap.get(CherryInput.PARAMETER_MAP_CONDITION_ONE_OF);
    parameter.feelOptional = getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_FEEL, contextInfo);

    List<Object> workerParameterChoiceList = (List) inputMap.get(CherryInput.PARAMETER_MAP_CHOICE_LIST);
    if (workerParameterChoiceList != null) {
      parameter.choiceList = new ArrayList<>();
      for (Object workerParameter : workerParameterChoiceList) {
        if (workerParameter instanceof Map workerParameterMap) {
          String code = getStringFromMap(workerParameterMap, CherryInput.PARAMETER_MAP_CHOICE_LIST_CODE,
              contextInfo + ".workerParameterChoiceList");
          String displayName = getStringFromMap(workerParameterMap, CherryInput.PARAMETER_MAP_CHOICE_LIST_DISPLAY_NAME,
              contextInfo + ".workerParameterChoiceList");
          WorkerParameterChoice workerParameterChoice = new WorkerParameterChoice(code, displayName);
          parameter.choiceList.add(workerParameterChoice);
        } else {
          logger.error("Error during transformList.workerParameterChoiceList : List Of Map expected, get {}",
              workerParameter == null ? "null" : workerParameter.getClass().getName());
        }
      }
    } // end workerParameterChoiceList != null
    parameter.visibleInTemplate = Boolean.TRUE.equals(inputMap.get(CherryInput.PARAMETER_MAP_VISIBLE_IN_TEMPLATE));
    // Retrieve Group
    String groupId = getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_GROUP_ID, contextInfo);
    String groupLabel = getStringFromMap(inputMap, CherryInput.PARAMETER_MAP_GROUP_LABEL, contextInfo);
    if (groupId != null) {
      parameter.group = new Group(groupId, groupLabel);
    }

    return parameter;
  }

  private static String getStringFromMap(Map<?, ?> map, String attributName, String contextInfo) {
    Object value = map.getOrDefault(attributName, null);
    if (value == null)
      return null;
    if (value instanceof String)
      return (String) value;

    logger.error("Error during getString in {}, attribut {} : String expected get {}", contextInfo, attributName,
        value.getClass().getName());
    return null;
  }

  public String getName() {
    return name;
  }

  public Level getLevel() {
    return level;
  }
  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Conditions                                              */
  /*                                                          */
  /* -------------------------------------------------------- */

  public boolean isAccessAllVariables() {
    return ACCESS_ALL_VARIABLES.equals(name);
  }

  /**
   * A Optional field may be systematically visible in the Template, to simplify the definition
   *
   * @return a runnerParameter where the attribute is set to true
   */
  public RunnerParameter setVisibleInTemplate() {
    this.visibleInTemplate = true;
    return this;
  }

  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Choice                                                  */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * Set the default value in the parameter
   *
   * @param defaultValue
   * @return a runnerParameter where the attribute is set to true
   */
  public RunnerParameter setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  public RunnerParameter setGsonTemplate(String gsonTemplate) {
    this.gsonTemplate = gsonTemplate;
    return this;
  }

  /**
   * Add a condition in the parameter
   *
   * @param property property to address the condition
   * @param oneOf    list of String to set the condition
   * @return a runnerParameter where the attribute is set to true
   */
  public RunnerParameter addCondition(String property, List<String> oneOf) {
    this.condition = property;
    this.conditionOneOf = oneOf;
    return this;
  }

  /**
   * return the condition property
   *
   * @return
   */
  public String getCondition() {
    return condition;
  }

  /**
   * Worker can define a list of choice. Add a new choice in the list
   *
   * @param name  name of the choice
   * @param label label to display to an user
   * @return Runner Parameter
   */
  public RunnerParameter addChoice(String name, String label) {
    if (choiceList == null)
      choiceList = new ArrayList<>();
    choiceList.add(new WorkerParameterChoice(name, label));
    return this;
  }


  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Group                                                   */
  /*                                                          */
  /* -------------------------------------------------------- */

  public boolean hasChoice() {
    return choiceList != null && !choiceList.isEmpty();
  }

  public List<String> getConditionOneOf() {
    return conditionOneOf;
  }

  /*
   * set the group to add in the template
   */
  public RunnerParameter setGroup(String label) {
    String groupId = label.toLowerCase().replace(" ", "_");

    this.group = new Group(groupId, label);
    return this;
  }

  public Map<String, Object> toMap(String parameterNameForCondition) {
    Map<String, Object> oneParameter = new HashMap<>();
    oneParameter.put(CherryInput.PARAMETER_MAP_NAME, name);
    oneParameter.put(CherryInput.PARAMETER_MAP_LABEL, label);
    oneParameter.put(CherryInput.PARAMETER_MAP_CLASS, clazz);
    oneParameter.put(CherryInput.PARAMETER_MAP_LEVEL, level.toString());
    oneParameter.put(CherryInput.PARAMETER_MAP_EXPLANATION, explanation);

    // Transform the choiceList to a List<Map> to avoid any class exception between the RunnerParameter embedded in
    // the JAR and the one in the Cherry Runtime
    if (choiceList != null && !choiceList.isEmpty()) {

      List<Map<String, Object>> choiceMapList = new ArrayList<>();
      for (WorkerParameterChoice choice : choiceList) {
        choiceMapList.add(Map.of(CherryInput.PARAMETER_MAP_CHOICE_LIST_CODE, choice.code,
            CherryInput.PARAMETER_MAP_CHOICE_LIST_DISPLAY_NAME, choice.displayName));
      }
      oneParameter.put(CherryInput.PARAMETER_MAP_CHOICE_LIST, choiceMapList);
    }
    oneParameter.put(CherryInput.PARAMETER_MAP_CONDITION, condition);
    oneParameter.put(CherryInput.PARAMETER_MAP_CONDITION_EQUALS, conditionEquals);

    oneParameter.put(CherryInput.PARAMETER_MAP_CONDITION_ONE_OF, conditionOneOf);

    if (group != null) {
      oneParameter.put(CherryInput.PARAMETER_MAP_GROUP_ID, group.id);
      oneParameter.put(CherryInput.PARAMETER_MAP_GROUP_LABEL, group.label);
    }
    if (defaultValue != null)
      oneParameter.put(CherryInput.PARAMETER_MAP_DEFAULT_VALUE, defaultValue.toString());

    oneParameter.put(CherryInput.PARAMETER_MAP_VISIBLE_IN_TEMPLATE, visibleInTemplate);

    logger.info("PdfParameters getMap:{}", oneParameter);

    return oneParameter;
  }



  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Serialization                                                   */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * Level on the parameter.
   * REQUIRED: a value must be given. in the element-template, a Mandatory is placed
   * OPTIONAL: value may be given, or not. In the element template, a checkbox is added, because in Modeler, not possible
   * to leave a empty field for some type, so something has to be done
   * FREE: not required, nothing is done in the element template, field will be visible
   */
  public enum Level {
    REQUIRED, OPTIONAL, FREE
  }

  /**
   * Parameter may define a list of choice.
   */
  public static class WorkerParameterChoice {
    public String code;
    public String displayName;

    public WorkerParameterChoice(String code, String displayName) {
      this.code = code;
      this.displayName = displayName;
    }
  }

  public record Group(String id, String label) {

    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof Group objGroup)
        return this.id.equals(objGroup.id);
      return false;
    }
  }

}
