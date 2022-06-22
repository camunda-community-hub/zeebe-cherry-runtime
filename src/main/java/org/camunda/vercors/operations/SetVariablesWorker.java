package org.camunda.vercors.operations;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import io.camunda.zeebe.spring.client.exception.ZeebeBpmnError;
import org.camunda.vercors.definition.AbstractWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Component
public class SetVariablesWorker extends AbstractWorker {


    public static final String BPMERROR_SYNTAXE_OPERATION_ERROR = "SYNTAX_OPERATION_ERROR";
    public static final String BPMERROR_DATEPARSE_OPERATION_ERROR = "DATEPARSE_OPERATION_ERROR";
    public static final String BPMERROR_UNKNOWFUNCTION_ERROR = "UNKNOWN_FUNCTION_ERROR";

    public static final String CST_NOW = "now";
    public static final String CST_ISODATE = "yyyy-MM-dd";
    public static final String CST_ISODATETIME =  "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String CST_FUNCTION_DATE = "date";
    public static final String CST_FUNCTION_DATETIME = "datetime";
    public static final String CST_FUNCTION_LOCALDATE = "localdate";
    public static final String CST_FUNCTION_LOCAL_TIME = "LocalTime";
    public static final String CST_FUNCTION_ZONED_DATE_TIME = "ZonedDateTime";
    private final static String INPUT_OPERATIONS = "operations";
    private final static String INPUT_ANYTHING = "*";
    private final static String OUTPUT_RESULT = "*";
    Logger logger = LoggerFactory.getLogger(SetVariablesWorker.class.getName());

    public SetVariablesWorker() {
        super("v-set-variables",
                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(INPUT_OPERATIONS, String.class, Level.REQUIRED, "Operations, example color=\"blue\";age=12;source=AnotherVariable. Each operation is separate by a semi colonne."),
                        AbstractWorker.WorkerParameter.getInstance(INPUT_ANYTHING, Object.class, Level.OPTIONAL, "Any variables can be accessed")
                ),
                Arrays.asList(
                        AbstractWorker.WorkerParameter.getInstance(OUTPUT_RESULT, Object.class, Level.REQUIRED, "Result of operations. Multiple variables are updated")
                ),
                Arrays.asList(BPMERROR_SYNTAXE_OPERATION_ERROR));
    }

    @Override
    @ZeebeWorker(type = "v-set-variables", autoComplete = true)
    public void handleWorkerExecution(final JobClient jobClient, final ActivatedJob activatedJob) {
        super.handleWorkerExecution(jobClient, activatedJob);
    }


    @Override
    public void execute(final JobClient jobClient, final ActivatedJob activatedJob, ContextExecution contextExecution) {
        String operations = getInputStringValue(INPUT_OPERATIONS, null, activatedJob);

        try {
            operations = operations.replaceAll("\\\\\"", "\"");
            StringTokenizer st = new StringTokenizer(operations, ";");
            while (st.hasMoreTokens()) {
                String oneOperationSt = st.nextToken();
                StringTokenizer oneOperation = new StringTokenizer(oneOperationSt, "=");
                String variableName = oneOperation.hasMoreTokens() ? oneOperation.nextToken() : "";
                String content = oneOperation.hasMoreTokens() ? oneOperation.nextToken() : null;
                // check the content: a String? A integer? A variable?
                if (content == null || content.isEmpty())
                    throw new ZeebeBpmnError(BPMERROR_SYNTAXE_OPERATION_ERROR, "Worker [" + getName() + "] Operation [" + oneOperationSt + "] must have name=value: value is missing.");
                Object contentVariable = null;
                FunctionDescription function = getFunction(content);
                if (function != null)
                    contentVariable = getValueFunction(function, activatedJob);
                else
                    contentVariable = getValue(content, activatedJob);


                // ok, now update the variable
                setValue(variableName, contentVariable, contextExecution);
            }
        } catch (Exception e) {
            throw new ZeebeBpmnError(BPMERROR_SYNTAXE_OPERATION_ERROR, "Worker [" + getName() + "] Syntax error on operation[" + operations + "] : " + e);
        }

    }

    /* ******************************************************************** */
    /*                                                                      */
    /*  Function                                                            */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * get, if we can detect a function, the function, else null
     *
     * @param content content which describe a function. Example : date(now)
     * @return the function description
     */
    private FunctionDescription getFunction(String content) {
        int begParenthesis = content.indexOf("(");
        int endParenthesis = content.lastIndexOf(")");
        if (begParenthesis == -1 || endParenthesis == -1) {
            // not a function
            if (begParenthesis != -1)
                logError("Incorrect function? function is <name>(<parameters>*) [" + content + "]");
            return null;
        }
        FunctionDescription function = new FunctionDescription();
        function.name = content.substring(0, begParenthesis);
        String param = content.substring(0, endParenthesis).substring(begParenthesis + 1);
        StringTokenizer st = new StringTokenizer(param, ",");
        while (st.hasMoreTokens()) {
            function.parameters.add(st.nextToken());
        }
        return function;
    }

    /**
     * GetValueFunction get the value via a function
     *
     * @param function function to use to get the value
     * @param activatedJob job activated
     * @return the value by the function
     * @throws Exception any errors arrive during the execution
     */
    private Object getValueFunction(FunctionDescription function, final ActivatedJob activatedJob) throws Exception {
        if (function.isFunction(CST_FUNCTION_DATE)) {
            Object dateValue = getValue(function.getParameter(0), activatedJob);
            if (CST_NOW.equals(dateValue) || dateValue == null)
                return new Date();
            else {
                try {
                    return new SimpleDateFormat(CST_ISODATE).parse(dateValue.toString());
                } catch (Exception e) {
                    throw new ZeebeBpmnError(BPMERROR_DATEPARSE_OPERATION_ERROR, "Worker [" + getName() + "] Can't parse date[" + dateValue + "] pattern [" + CST_ISODATE + "]: " + e);
                }
            }
        }
        else if (function.isFunction(CST_FUNCTION_DATETIME)) {
            Object dateValue = getValue(function.getParameter(0), activatedJob);
            if (CST_NOW.equals(dateValue) || dateValue == null)
                return new Date();
            else
                try {
                    return new SimpleDateFormat(CST_ISODATETIME).parse(dateValue.toString());
                } catch (Exception e) {
                    throw new ZeebeBpmnError(BPMERROR_DATEPARSE_OPERATION_ERROR, "Worker [" + getName() + "] Can't parse date[" + dateValue + "] pattern [" + CST_ISODATETIME + "]: " + e);
                }
        }
        else if (function.isFunction(CST_FUNCTION_LOCALDATE)) {
            Object dateValue = getValue(function.getParameter(0), activatedJob);
            if (CST_NOW.equals(dateValue) || dateValue == null)
                return LocalDate.now();
            else
                try {
                    return LocalDate.parse(dateValue.toString());
                } catch (Exception e) {
                    throw new ZeebeBpmnError(BPMERROR_DATEPARSE_OPERATION_ERROR, "Worker [" + getName() + "] Can't parse date[" + dateValue + "] LocalDate pattern [yyyy-MM-dd]: " + e);
                }
        }
        else if (function.isFunction(CST_FUNCTION_LOCAL_TIME)) {
            Object dateValue = getValue(function.getParameter(0), activatedJob);
            if (CST_NOW.equals(dateValue) || dateValue == null)
                return LocalDate.now();
            else
                try {
                    return LocalDateTime.parse(dateValue.toString());
                } catch (Exception e) {
                    throw new ZeebeBpmnError(BPMERROR_DATEPARSE_OPERATION_ERROR, "Worker [" + getName() + "] Can't parse date[" + dateValue + "] LocalDate pattern [yyyy-MM-dd'T'HH:mm:ss'Z']: " + e);
                }
        }
        else if (function.isFunction(CST_FUNCTION_ZONED_DATE_TIME)) {
            Object dateValue = getValue(function.getParameter(0), activatedJob);
            if (CST_NOW.equals(dateValue) || dateValue == null)
                return ZonedDateTime.now();
            else
                try {
                    return ZonedDateTime.parse(dateValue.toString());
                } catch (Exception e) {
                    throw new ZeebeBpmnError(BPMERROR_DATEPARSE_OPERATION_ERROR, "Worker [" + getName() + "] Can't parse date[" + dateValue + "] LocalDate pattern [yyyy-MM-dd'T'HH:mm:ss[+-]hh:mm]: " + e);
                }

        } else {
            throw new ZeebeBpmnError(BPMERROR_UNKNOWFUNCTION_ERROR, "Worker [" + getName() + "] function["+function.name + "] unknown");

        }
    }

    /**
     * Return the value. It may be a String, or a variable
     *
     * @param valueSt      value to return
     * @param activatedJob job to get variable
     * @return
     */
    private Object getValue(String valueSt, final ActivatedJob activatedJob) {
        if (valueSt == null)
            return null;

        if (valueSt.startsWith("\"")) {
            if (!valueSt.endsWith("\""))
                throw new ZeebeBpmnError(BPMERROR_SYNTAXE_OPERATION_ERROR, "Worker [" + getName() + "] Operation [" + valueSt + "]: String must start and end by a \" ");
            return valueSt.substring(0, valueSt.length() - 1).substring(1);
        }

        // try to parse different format
        try {
            return Integer.parseInt(valueSt);
        } catch (Exception e) {
            // do nothing, we try a different format
        }
        try {
            return Long.parseLong(valueSt);
        } catch (Exception e) {
            // do nothing, we try a different format
        }
        try {
            return Double.parseDouble(valueSt);
        } catch (Exception e) {
            // do nothing, we try a different format
        }

        return activatedJob.getVariablesAsMap().get(valueSt);
    }



    /* ******************************************************************** */
    /*                                                                      */
    /*  Value                                                               */
    /*                                                                      */
    /* ******************************************************************** */

    /**
     * Function implementation
     */
    public static class FunctionDescription {
        public String name;
        public List<String> parameters = new ArrayList<>();

        public boolean isFunction(String compareName) {
            return compareName.equalsIgnoreCase(name);
        }

        public String getParameter(int range) {
            return range < parameters.size() ? parameters.get(range) : null;

        }
    }

}
