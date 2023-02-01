/* ******************************************************************** */
/*                                                                      */
/*  WorkerParameter                                                     */
/*                                                                      */
/*  Define a parameters (Worker or Connector)                           */
/* ******************************************************************** */
package io.camunda.cherry.definition;

import java.util.ArrayList;
import java.util.List;

public class RunnerParameter {

  public static final String ACCESS_ALL_VARIABLES = "*";
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
  /**
   * Declare a condition on the parameters
   */
  public String conditionProperty;
  public List<String> conditionOneOf;
  public List<WorkerParameterChoice> workerParameterChoiceList;
  public boolean visibleInTemplate = false;
  public Group group;

  /**
   * Get an instance without a default value
   *
   * @param parameterName parameter name
   * @param clazz         class of the expected parameter
   * @param defaultValue  the default value for this parameter, if no value is given. Note: a required parameter may have a null as a value.
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
   * A Gson Parameter can be given as a String or at a Object?
   *
   * @param parameterName  parameter name
   * @param parameterLabel label to display in the template
   * @param level          level for this parameter
   * @param explanation    describe the usage of the parameter
   * @param gsonTemplate   Give an example of the format, to verify that the parameters meet the requirement. Format is <Parameter>:<Class>/Example {"host":"String", "Port": "Integer"}
   * @return
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
   * @return
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

  public String getName() {
    return name;
  }

  public Level getLevel() {
    return level;
  }

  public boolean isAccessAllVariables() {
    return ACCESS_ALL_VARIABLES.equals(name);
  }

  /**
   * A Optional field may be systematically visible in the Template, to simplify the definition
   *
   * @return
   */
  public RunnerParameter setVisibleInTemplate() {
    this.visibleInTemplate = true;
    return this;
  }

  public RunnerParameter setDefaultValue(Object defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }
  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Conditions                                              */
  /*                                                          */
  /* -------------------------------------------------------- */

  public RunnerParameter addCondition(String property, List<String> oneOf) {
    this.conditionProperty = property;
    this.conditionOneOf = oneOf;
    return this;
  }


  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Choice                                                  */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * Worker can define a list of choice. Add a new choice in the list
   *
   * @param name  name of the choice
   * @param label label to display to an user
   * @return Runner Parameter
   */
  public RunnerParameter addChoice(String name, String label) {
    if (workerParameterChoiceList == null)
      workerParameterChoiceList = new ArrayList<>();
    workerParameterChoiceList.add(new WorkerParameterChoice(name, label));
    return this;
  }

  public boolean hasChoice() {
    return workerParameterChoiceList != null && !workerParameterChoiceList.isEmpty();
  }

  /*
   * set the group to add in the template
   */
  public RunnerParameter setGroup(String label) {
    String groupId = label.toLowerCase().replace(" ", "_");

    this.group = new Group(groupId, label);
    return this;
  }


  /* -------------------------------------------------------- */
  /*                                                          */
  /*  Group                                                   */
  /*                                                          */
  /* -------------------------------------------------------- */

  /**
   * Level on the parameter.
   */
  public enum Level {REQUIRED, OPTIONAL}

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
