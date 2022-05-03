package org.camunda.vercors.definition;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;

import java.util.*;
import java.util.stream.Collectors;

public abstract class WorkerBase {

    private final String name;
    private final List<WorkerParameter> listInput;
    private final List<WorkerParameter> listOutput;

    public WorkerBase(String name,
                      List<WorkerParameter> listInput,
                      List<WorkerParameter> listOutput) {
        this.name = name;
        this.listInput = listInput;
        this.listOutput = listOutput;
    }
    private final Map<String,String> outVariables = new HashMap<>();
    /**
     * The connector must call immediately this method, which is the skeleton for all executions
     */
    public void handleWorkerExecution(final JobClient client, final ActivatedJob job)  {
        try {
            // first, see if the process respect the contract for this connector
            checkInput( job );

            // ok, this is correct, execute it now
            execute(client, job);

            // let's verify the execution respect the output contract
            checkOutput();
        }
        catch(Exception e) {
            // throw a BPMN Error
        }
    }

    public abstract void execute(final JobClient client, final ActivatedJob job);


    /**
     * Check the contract
     * Each connector must and return a contract for what it needs for the execution
     * @throws RuntimeException if the input is incorrect, contract not respected
     */
    private void checkInput(final ActivatedJob job) throws RuntimeException {
        List<String> listErrors = new ArrayList<>();
        for (WorkerParameter parameter : listInput) {
            if (job.getVariablesAsMap().containsKey(parameter.name)) {
                Object value = job.getVariablesAsMap().get(parameter.name);
                if (value != null && !value.getClass().equals(parameter.clazz)) {
                    listErrors.add("Param[" + parameter.name + "] expect class[" + parameter.clazz.getName() + "] received[" + value.getClass() + "];");
                }
            } else if (parameter.level == Level.REQUIRED) {
                listErrors.add("Param[" + parameter.name + "] is missing");
            }
        }
        if (! listErrors.isEmpty()) {
            throw new ZeebeBpmnError("INPUTCONTRACT_EXCEPTION","Worker ["+name+"] InputContract Exception:"+String.join(",", listErrors));
        }
    }

    /**
     * Check the contract
     * The connector must use setVariable to set any value. Then, we can verify that all expected information are provided
     * @throws RuntimeException when the contract is not respected
     */
    private void checkOutput() throws RuntimeException{
        List<String> listErrors = new ArrayList<>();

        for (WorkerParameter parameter : listOutput) {

            if (parameter.level == Level.REQUIRED) {
                if (!outVariables.containsKey(parameter.name))
                    listErrors.add("Param[" + parameter.name + "] is missing");
            }
            // if the value is given, it must be the correct value
            if (outVariables.containsKey(parameter.name)) {
                if (outVariables.get(parameter.name) != null
                        && !outVariables.get(parameter.name).equals(parameter.clazz.getName()))
                    listErrors.add("Param[" + parameter.name + "] expect class[" + parameter.clazz.getName()
                            + "] received[" + outVariables.get(parameter.name).getClass() + "];");
            }
        }
        Set<String> outputName = listOutput.stream().map(t->t.name).collect(Collectors.toSet());
        // second pass: verify that the connector does not provide an unexpected value
        List<String> listExtraVariables = outVariables.keySet()
                .stream()
                .filter( outputName::contains )
                .collect(Collectors.toList());
        if (! listExtraVariables.isEmpty())
            listErrors.add("Output not defined in the contract[" + String.join(",", listExtraVariables)+"]");

        if (! listErrors.isEmpty()) {
            throw new ZeebeBpmnError("OUTPUTCONTRACT_EXCEPTION", "Worker["+name+"] OutputContract Exception:"+String.join(",", listErrors));
        }
    }

    public enum Level {REQUIRED,OPTIONAL}
    public static class WorkerParameter {
        public String name;
        public Class clazz;
        public Level level;
        public static WorkerParameter getInstance(String name, Class clazz, Level level) {
            WorkerParameter parameter =new WorkerParameter();
            parameter.name = name;
            parameter.clazz = clazz;
            parameter.level = level;
            return parameter;
        }
    }
    /**
     * Retrieve a variable, and return the string representation. If the variable is not a String, then a toString() is returned. If the value does not exist, then defaultValue is returned
     * The method can return null if the variable exists, but it is a null value.
     *
     * @param name name of the variable to load
     * @param defaultValue if the input does not exist, this is the default value.
     * @param job job passed to the worker
     * @return the value as String
     */
    public String getInputStringValue(String name, String defaultValue, final ActivatedJob job) {
        if (! job.getVariablesAsMap().containsKey(name))
            return defaultValue;
        Object value = job.getVariablesAsMap().get(name);
        return value == null ? null : value.toString();
    }

    /**
     *
     * @param name name of the input value
     * @param defaultValue if the input does not exist, this is the default value.
     * @param job job passed to the worker
     * @return the value as String
     */
    public Object getValue(String name, Object defaultValue, ActivatedJob job) {
        if (! job.getVariablesAsMap().containsKey(name))
            return defaultValue;
        try {
            return job.getVariablesAsMap().get(name);
        } catch(Exception e) {
            return defaultValue;
        }
    }

    public void setValue(String name, Object value) {
        outVariables.put(name, value==null ? null : value.getClass().getName());
    }
}
