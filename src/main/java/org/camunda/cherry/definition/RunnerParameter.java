/* ******************************************************************** */
/*                                                                      */
/*  WorkerParameter                                                     */
/*                                                                      */
/*  Define a parameters (Worker or Connector)                           */
/* ******************************************************************** */
package org.camunda.cherry.definition;

import java.util.ArrayList;
import java.util.List;

public class RunnerParameter {

    /**
     * Level on the parameter.
     */
    public enum Level {REQUIRED, OPTIONAL}

    /**
     * Parameter may define a list of choice.
     */
    public static class WorkerParameterChoice {
        public String name;
        public String label;

        public WorkerParameterChoice(String name, String label) {
            this.name = name;
            this.label = label;
        }
    }

    /**
     * class to declare a parameter
     */
    public String name;
    public String label;
    public Class<?> clazz;
    public Object defaultValue;
    public Level level;
    public String explanation;


    /**
     * Declare a condition on the parameters
     */
    public String conditionProperty;
    public List<String> conditionOneOf;


    public List<WorkerParameterChoice> workerParameterChoiceList;

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
                                              String parameterLabel, Class<?> clazz,
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

    public RunnerParameter addCondition(String property, List<String> oneOf) {
        this.conditionProperty = property;
        this.conditionOneOf = oneOf;
        return this;
    }

    /**
     * Worker can define a list of choice. Add a new choice in the list
     *
     * @param name name of the choice
     * @param label label to display to an user
     * @return Runner Parameter
     */
    public RunnerParameter addChoice(String name, String label) {
        if (workerParameterChoiceList == null)
            workerParameterChoiceList = new ArrayList<>();
        workerParameterChoiceList.add(new WorkerParameterChoice(name, label));
        return this;
    }

    public String getName() {
        return name;
    }

}
