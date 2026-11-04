/* ******************************************************************** */
/*                                                                      */
/*  RunnerLightDefinition                                               */
/*                                                                      */
/*  To carry information on different Runner                            */
/*                                                                      */
/* ******************************************************************** */
package io.camunda.cherry.runner;

import io.camunda.cherry.db.entity.RunnerDefinitionEntity;

public class RunnerLightDefinition {

    private final String type;

    private final String name;
    private final RunnerDefinitionEntity.Origin origin;
    private final String classname;

    String release;

    public RunnerLightDefinition(String name, String type, String className, RunnerDefinitionEntity.Origin origin, String release) {
        this.name = name;
        this.type = type;
        this.classname = className;
        this.origin = origin;
        this.release = release;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getClassname() {
        return classname;
    }

    public RunnerDefinitionEntity.Origin getOrigin() {
        return origin;
    }
}
