/* ******************************************************************** */
/*                                                                      */
/*  RunnerDecorationTemplate                                            */
/*                                                                      */
/*  Generate the template file used in Modeler from a runner            */
/* ******************************************************************** */
package io.camunda.cherry.definition;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunnerDecorationTemplate {

  /*    We want to keep the Output parameter at the end.
   * when a field has a group in the list, it is placed in top.
   * When a field does not have a group, it is placed after in a group "custom properties"
   * so, we assign a Input group or an Output group by default to each field.
   * */

  public static final String GROUP_OUTPUT = "Output";
  public static final String GROUP_OUTPUT_LABEL = "Output";
  public static final String GROUP_INPUT = "Input";
  public static final String GROUP_INPUT_LABEL = "Input";
  public static final String ATTR_LABEL = "label";
  public static final String ATTR_TYPE = "type";
  public static final String ZEEBE_TASK_HEADER = "zeebe:taskHeader";
  public static final String ATTR_GROUPS = "groups";
  public static final String ATTR_TYPE_HIDDEN = "Hidden";
  public static final String ATTR_KEY = "key";
  public static final String ATTR_VALUE = "value";
  public static final String ATTR_DESCRIPTION = "description";
  public static final String ATTR_BINDING = "binding";
  public static final String ATTR_KEY_RESULT_VARIABLE = "Result Variable";
  public static final String ZEEBE_TASK_DEFINITION_TYPE = "zeebe:taskDefinition:type";
  public static final String RESULT_VARIABLE = "result";
  public static final String ATTR_NAME = "name";
  public static final String ATTR_ID = "id";
  public static final String ATTR_DOCUMENTATION_REF = "documentationRef";
  public static final String ATTR_ICON = "icon";
  public static final String ATTR_CATEGORY = "category";
  public static final String ATTR_APPLIES_TO = "appliesTo";
  public static final String ATTR_ELEMENT_TYPE = "elementType";
  public static final String ATTR_PROPERTIES = "properties";
  public static final String ATTR_CHOICES = "choices";
  public static final String ATTR_CONDITION = "condition";
  public static final String ATTR_GROUP = "group";
  public static final String TYPE_FIELD_STRING = "String";
  public static final String TYPE_FIELD_DROPDOWN = "Dropdown";
  public static final String ATTR_CONSTRAINTS_NOT_EMPTY = "notEmpty";
  public static final String ATTR_CONSTRAINTS = "constraints";

  private final AbstractRunner runner;

  public RunnerDecorationTemplate(AbstractRunner runner) {
    this.runner = runner;
  }

  public static String getJsonFromList(List<Map<String, Object>> listTemplates) {
    // transform the result in JSON
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(listTemplates);
  }

  /**
   * produce a JSON string containing the definition for the template
   * https://docs.camunda.io/docs/components/modeler/desktop-modeler/element-templates/defining-templates/
   *
   * @return the template
   */
  public Map<String, Object> getTemplate() {

    Map<String, Object> templateContent = new HashMap<>();
    if (!runner.checkValidDefinition().listOfErrors().isEmpty())
      return templateContent;

    templateContent.put("$schema",
        "https://unpkg.com/@camunda/zeebe-element-templates-json-schema/resources/schema.json");
    templateContent.put(ATTR_NAME, runner.getDisplayLabel());
    templateContent.put(ATTR_ID, runner.getClass().getName());
    templateContent.put(ATTR_DESCRIPTION, runner.getDescription());
    templateContent.put(ATTR_DOCUMENTATION_REF,
        "https://docs.camunda.io/docs/components/modeler/web-modeler/connectors/available-connectors/template/");
    if (runner.getLogo() != null)
      templateContent.put(ATTR_ICON, Map.of("contents", runner.getLogo()));
    templateContent.put(ATTR_CATEGORY, Map.of(ATTR_ID, "connectors", ATTR_NAME, "connectors"));
    templateContent.put(ATTR_APPLIES_TO, List.of("bpmn:Task"));
    templateContent.put(ATTR_ELEMENT_TYPE, Map.of(ATTR_VALUE, "bpmn:ServiceTask"));
    // no groups at this moment
    List<Map<String, Object>> listProperties = new ArrayList<>();
    templateContent.put(ATTR_PROPERTIES, listProperties);
    listProperties.add(Map.of(ATTR_TYPE, ATTR_TYPE_HIDDEN, // Hidden
        ATTR_VALUE, runner.getType(), // Default value
        ATTR_BINDING, Map.of(ATTR_TYPE, ZEEBE_TASK_DEFINITION_TYPE)));
    boolean pleaseAddOutputGroup = false;

    // Do not ask multiple time the listInput
    List<RunnerParameter> listInput = runner.getListInput();
    List<RunnerParameter> listOutput = runner.getListOutput();

    // AbstractConnector
    if (runner instanceof AbstractConnector) {
      pleaseAddOutputGroup = true;

      // there is here two options:
      // connector return and object or a list of output
      if (listInput.isEmpty()) {
        // connector returns an object
        listProperties.add(Map.of( // list of properties
            ATTR_LABEL, "Result Variable Label", // Label
            ATTR_TYPE, TYPE_FIELD_STRING, ATTR_VALUE, RESULT_VARIABLE, // Variable Value Name
            ATTR_GROUPS, "output", // set in the output group
            ATTR_BINDING, Map.of(ATTR_TYPE, ZEEBE_TASK_HEADER, ATTR_KEY, ATTR_KEY_RESULT_VARIABLE)));
      } else {
        listProperties.add(Map.of( // list of properties
            ATTR_TYPE, ATTR_TYPE_HIDDEN, // Hidden, because one field is created per output
            ATTR_VALUE, RESULT_VARIABLE, // save the result in the result variable
            ATTR_BINDING, Map.of(ATTR_TYPE, ZEEBE_TASK_HEADER, ATTR_KEY, ATTR_KEY_RESULT_VARIABLE)));
      }
    }

    // Identify all groups
    List<RunnerParameter.Group> listGroups = new ArrayList<>();
    listGroups.addAll(listInput.stream().filter(w -> w.group != null).map(w -> w.group).toList());

    // We group all result in a Group Input
    if (!listInput.isEmpty())
      listGroups.add(new RunnerParameter.Group(GROUP_INPUT, GROUP_INPUT_LABEL));

    // We group all result in a Group Output
    if (!listOutput.isEmpty() || pleaseAddOutputGroup)
      listGroups.add(new RunnerParameter.Group(GROUP_OUTPUT, GROUP_OUTPUT_LABEL));

    if (listGroups != null) {
      templateContent.put(ATTR_GROUPS,
          listGroups.stream().distinct().map(w -> Map.of(ATTR_ID, w.id(), ATTR_LABEL, w.label())).toList());
    }

    for (RunnerParameter runnerParameter : listInput) {
      // do not generate a propertie for a accessAllVariables
      if (runnerParameter.isAccessAllVariables())
        continue;
      listProperties.addAll(getParameterProperties(runnerParameter, true, ""));
    }
    for (RunnerParameter runnerParameter : listOutput) {
      // do not generate a property for accessAllVariables
      if (runnerParameter.isAccessAllVariables())
        continue;
      listProperties.addAll(getParameterProperties(runnerParameter, false,
          runner instanceof AbstractConnector ? RESULT_VARIABLE + "." : ""));
    }

    // check if the runner generates error
    if (!runner.getListBpmnErrors().isEmpty()) {
      //
      //            "label": "ControllerPage Expression",
      //            "description": "Expression to define BPMN Errors to throw",
      //            "group": "errors",
      //            "type": "Hidden",
      //            "value": "bpmnError(error.code, error.message)",
      //            "binding": {
      //            "type": "zeebe:taskHeader",
      //                "key": "errorExpression"
      //
      HashMap<String, Object> errorParameters = new HashMap<>();
      errorParameters.put(ATTR_LABEL, "ControllerPage Expression");
      errorParameters.put(ATTR_DESCRIPTION, "Expression to define BPMN Errors to throw");
      errorParameters.put(ATTR_TYPE, ATTR_TYPE_HIDDEN);
      errorParameters.put(ATTR_VALUE, "if is defined(error) then bpmnError(error.code, error.message) else null");
      errorParameters.put(ATTR_BINDING,
          Map.of(ATTR_TYPE, ZEEBE_TASK_HEADER, ATTR_KEY, "ERROR_EXPRESSION_KEYWORD"));

      listProperties.add(errorParameters);
    }

    return templateContent;
  }

  /**
   * Get the template from a runnerParameter
   *
   * @param runnerParameter runner parameter to get the description
   * @param isInput         true if this is an input parameter
   * @param prefixName      add a prefixName to the source in the binding (mandatory for connectors)
   * @return a template description
   */
  private List<Map<String, Object>> getParameterProperties(RunnerParameter runnerParameter,
                                                           boolean isInput,
                                                           String prefixName) {
    List<Map<String, Object>> listProperties = new ArrayList<>();

    // Calculate the condition
    HashMap<String, Object> condition = null;
    if (runnerParameter.conditionProperty != null) {
      condition = new HashMap<>();
      condition.put("property", runnerParameter.conditionProperty);
      condition.put("oneOf", runnerParameter.conditionOneOf);
    }
    /** To have a checkbox, the parameter must be optionnal AND does not have already a condition */
    boolean addConditionCheckbox =
        (runnerParameter.conditionProperty == null) && (RunnerParameter.Level.OPTIONAL.equals(
            runnerParameter.getLevel()));

    if (runnerParameter.visibleInTemplate)
      addConditionCheckbox = false;
    // Add the condition for all output
    if (!isInput)
      addConditionCheckbox = true;

    // is the parameter is optional? Add a checkbox first
    if (addConditionCheckbox) {
      Map<String, Object> propertyCheckbox = new HashMap<>();
      listProperties.add(propertyCheckbox);
      // the ID property is the value to link with the conditional
      propertyCheckbox.put(ATTR_ID, runnerParameter.name + "_optional");
      if (isInput)
        propertyCheckbox.put(ATTR_LABEL, "Provide " + runnerParameter.label + "?");
      else
        propertyCheckbox.put(ATTR_LABEL, "Saved " + runnerParameter.label + "?");
      // don't have the group at this moment
      propertyCheckbox.put(ATTR_DESCRIPTION, runnerParameter.explanation);
      propertyCheckbox.put(ATTR_VALUE, "false");
      propertyCheckbox.put(ATTR_BINDING,
          Map.of(ATTR_TYPE, "zeebe:input", ATTR_NAME, runnerParameter.name + "_optional"));

      propertyCheckbox.put(ATTR_TYPE, TYPE_FIELD_DROPDOWN);
      List<Map<String, String>> listYesNoChoices = new ArrayList<>();
      listYesNoChoices.add(Map.of(ATTR_NAME, "Yes", ATTR_VALUE, "true"));
      listYesNoChoices.add(Map.of(ATTR_NAME, "No", ATTR_VALUE, "false"));
      propertyCheckbox.put(ATTR_CHOICES, listYesNoChoices);

      // if the parameters has a condition, add it here
      if (condition != null)
        propertyCheckbox.put(ATTR_CONDITION, condition);
      if (runnerParameter.group != null)
        propertyCheckbox.put(ATTR_GROUP, runnerParameter.group.id());
      else
        propertyCheckbox.put(ATTR_GROUP, isInput ? GROUP_INPUT : GROUP_OUTPUT);
    }

    Map<String, Object> propertyParameter = new HashMap<>();
    listProperties.add(propertyParameter);
    propertyParameter.put(ATTR_ID, runnerParameter.name);
    propertyParameter.put(ATTR_LABEL, runnerParameter.label);
    // don't have the group at this moment
    propertyParameter.put(ATTR_DESCRIPTION, runnerParameter.explanation);
    if (runnerParameter.defaultValue != null) {
      propertyParameter.put(ATTR_VALUE, runnerParameter.defaultValue);
    }
    String typeParameter = TYPE_FIELD_STRING;
    // String, Text, Boolean, Dropdown or Hidden)
    if (Boolean.class.equals(runnerParameter.clazz)) {
      typeParameter = TYPE_FIELD_DROPDOWN;
      List<Map<String, String>> listYesNoChoices = new ArrayList<>();
      listYesNoChoices.add(Map.of(ATTR_NAME, "Yes", ATTR_VALUE, "true"));
      listYesNoChoices.add(Map.of(ATTR_NAME, "No", ATTR_VALUE, "false"));
      propertyParameter.put(ATTR_CHOICES, listYesNoChoices);
    } else if (runnerParameter.hasChoice()) {
      typeParameter = TYPE_FIELD_DROPDOWN;
      // add choices
      List<Map<String, String>> listChoices = new ArrayList<>();
      for (RunnerParameter.WorkerParameterChoice oneChoice : runnerParameter.workerParameterChoiceList) {
        listChoices.add(Map.of(ATTR_NAME, oneChoice.displayName, ATTR_VALUE, oneChoice.code));
      }
      propertyParameter.put(ATTR_CHOICES, listChoices);
    }
    propertyParameter.put(ATTR_TYPE, typeParameter);
    if (isInput) {
      propertyParameter.put(ATTR_BINDING, Map.of(ATTR_TYPE, "zeebe:input", ATTR_NAME, runnerParameter.name));
    } else {
      propertyParameter.put(ATTR_BINDING,
          Map.of(ATTR_TYPE, "zeebe:output", "source", "= " + prefixName + runnerParameter.name));
    }
    if (runnerParameter.group != null)
      propertyParameter.put(ATTR_GROUP, runnerParameter.group.id());
    else
      propertyParameter.put(ATTR_GROUP, isInput ? GROUP_INPUT : GROUP_OUTPUT);

    Map<String, Object> constraints = new HashMap<>();
    // if the designer decide to show this property, then it is mandatory
    if (!isInput)
      constraints.put(ATTR_CONSTRAINTS_NOT_EMPTY, Boolean.TRUE);

    if (RunnerParameter.Level.REQUIRED.equals(runnerParameter.level))
      constraints.put(ATTR_CONSTRAINTS_NOT_EMPTY, Boolean.TRUE);

    if (!constraints.isEmpty())
      propertyParameter.put(ATTR_CONSTRAINTS, constraints);

    // if this is a OPTIONAL, then the display depends on the check box.
    // if there is a condition on the OPTIONAL, then the condition is part of the checkbox, else
    // will be on the parameters
    if (addConditionCheckbox) {
      propertyParameter.put(ATTR_CONDITION, Map.of("property", runnerParameter.name + "_optional", "equals", "true"));

    } else {
      if (condition != null)
        propertyParameter.put(ATTR_CONDITION, condition);
    }

    return listProperties;
  }
}
