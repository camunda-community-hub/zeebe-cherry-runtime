/* ******************************************************************** */
/*                                                                      */
/*  RunnerDecorationTemplate                                            */
/*                                                                      */
/*  Generate the template file used in Modeler from a runner            */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunnerDecorationTemplate {

    private final AbstractRunner runner;

    public RunnerDecorationTemplate(AbstractRunner runner) {
        this.runner = runner;
    }

    public static String getJsonFromList(List<Map<String, Object>> listTemplates) {
        // transform the result in JSON
        Gson gson = new Gson();
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
        templateContent.put("$schema", "https://unpkg.com/@camunda/zeebe-element-templates-json-schema/resources/schema.json");
        templateContent.put("name", runner.getDisplayLabel());
        templateContent.put("id", runner.getClass().getName());
        templateContent.put("description", runner.getDescription());
        templateContent.put("documentationRef", "https://docs.camunda.io/docs/components/modeler/web-modeler/connectors/available-connectors/template/");
        if (runner.getLogo() != null)
            templateContent.put("icon", Map.of("contents", runner.getLogo()));
        templateContent.put("category", Map.of("id", "connectors", "name", "connectors"));
        templateContent.put("appliesTo", List.of("bpmn:Task"));
        templateContent.put("elementType", Map.of("value", "bpmn:ServiceTask"));
        // no groups at this moment
        List<Map<String, Object>> listProperties = new ArrayList<>();
        templateContent.put("properties", listProperties);
        listProperties.add(
                Map.of("type", "Hidden",
                        "value", runner.getType(),
                        "binding", Map.of("type", "zeebe:taskDefinition:type")));

        // Identify all groups
        List<RunnerParameter.Group> listGroups = new ArrayList<>();
        listGroups.addAll(runner.getListInput().stream()
                .filter(w -> w.group != null)
                .map(w -> w.group)
                .toList());
        listGroups.addAll(runner.getListOutput().stream()
                .filter(w -> w.group != null)
                .map(w -> w.group)
                .toList());
        if (listGroups != null) {
            templateContent.put("groups",
                    listGroups.stream()
                            .distinct()
                            .map(w -> Map.of("id", w.id(), "label", w.label()))
                            .toList());
        }

        for (RunnerParameter runner : runner.getListInput()) {
            // do not generate a propertie for a accessAllVariables
            if (runner.isAccessAllVariables())
                continue;
            listProperties.addAll(getParameterProperties(runner, true));
        }
        for (RunnerParameter runner : runner.getListOutput()) {
            // do not generate a propertie for a accessAllVariables
            if (runner.isAccessAllVariables())
                continue;
            listProperties.addAll(getParameterProperties(runner, false));
        }
        return templateContent;
    }

    /**
     * Get the template from a runnerParameter
     *
     * @param runnerParameter runner parameter to get the description
     * @param isInput         true if this is an input parameter
     * @return a template description
     */
    private List<Map<String, Object>> getParameterProperties(RunnerParameter runnerParameter, boolean isInput) {
        List<Map<String, Object>> listProperties = new ArrayList<>();

        // Calculate the condition
        HashMap<String, Object> condition = null;
        if (runnerParameter.conditionProperty != null) {
            condition = new HashMap<>();
            condition.put("property", runnerParameter.conditionProperty);
            condition.put("oneOf", runnerParameter.conditionOneOf);
        }
        /**
         * To have a checkbox, the parameter must be optionnal AND does not have already a condition
         */
        boolean addConditionCheckbox = (runnerParameter.conditionProperty == null) &&
                (RunnerParameter.Level.OPTIONAL.equals(runnerParameter.getLevel()));

        if (runnerParameter.visibleInTemplate)
            addConditionCheckbox = false;

        // is the parameter is optional? Add a checkbox first
        if (addConditionCheckbox) {
            Map<String, Object> propertyCheckbox = new HashMap<>();
            listProperties.add(propertyCheckbox);
            propertyCheckbox.put("id", runnerParameter.name + "_optional");
            propertyCheckbox.put("label", "Provide " + runnerParameter.label + "?");
            // don't have the group at this moment
            propertyCheckbox.put("description", runnerParameter.explanation);
            propertyCheckbox.put("type", "Dropdown");
            propertyCheckbox.put("value", "false");
            propertyCheckbox.put("binding", Map.of("type", "zeebe:input",
                    "name", runnerParameter.name + "_optional"));

            List<Map<String, String>> listYesNoChoices = new ArrayList<>();
            listYesNoChoices.add(Map.of("name", "Yes", "value", "true"));
            listYesNoChoices.add(Map.of("name", "No", "value", "false"));

            propertyCheckbox.put("choices", listYesNoChoices);
            // if the parameters has a condition, add it here
            if (condition != null)
                propertyCheckbox.put("condition", condition);
            if (runnerParameter.group != null)
                propertyCheckbox.put("group", runnerParameter.group.id());
        }

        Map<String, Object> propertyParameter = new HashMap<>();
        listProperties.add(propertyParameter);
        propertyParameter.put("id", runnerParameter.name);
        propertyParameter.put("label", runnerParameter.label);
        // don't have the group at this moment
        propertyParameter.put("description", runnerParameter.explanation);
        if (runnerParameter.defaultValue != null) {
            propertyParameter.put("value", runnerParameter.defaultValue);
        }
        String typeParameter = "String";
        // String, Text, Boolean, Dropdown or Hidden)
        if (Boolean.class.equals(runnerParameter.clazz))
            typeParameter = "Boolean";
        else if (runnerParameter.hasChoice()) {
            typeParameter = "Dropdown";
            // add choices
            List<Map<String, String>> listChoices = new ArrayList<>();
            for (RunnerParameter.WorkerParameterChoice oneChoice : runnerParameter.workerParameterChoiceList) {
                listChoices.add(Map.of("name", oneChoice.displayName,
                        "value", oneChoice.code));
            }
            propertyParameter.put("choices", listChoices);
        }
        propertyParameter.put("type", typeParameter);
        if (isInput) {
            propertyParameter.put("binding",
                    Map.of("type", "zeebe:input",
                            "name", runnerParameter.name));
        } else {
            propertyParameter.put("binding",
                    Map.of("type", "zeebe:output",
                            "source", "= " + runnerParameter.name));

        }
        if (runnerParameter.group != null)
            propertyParameter.put("group", runnerParameter.group.id());

        Map<String, Object> constraints = new HashMap<>();
        if (runnerParameter.level == RunnerParameter.Level.REQUIRED)
            constraints.put("notEmpty", Boolean.TRUE);

        if (!constraints.isEmpty())
            propertyParameter.put("constraints", constraints);

        // if this is a OPTIONAL, then the display depends on the check box.
        // if there is a condition on the OPTIONAL, then the condition is part of the checkbox, else will be on the parameters
        if (addConditionCheckbox) {
            propertyParameter.put("condition", Map.of("property", runnerParameter.name + "_optional",
                    "equals", "true"));

        } else {
            if (condition != null)
                propertyParameter.put("condition", condition);

        }


        return listProperties;
    }
}
