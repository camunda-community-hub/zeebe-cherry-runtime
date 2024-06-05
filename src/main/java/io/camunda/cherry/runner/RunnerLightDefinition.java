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

  RunnerLightDefinition(String name, String type, String className, RunnerDefinitionEntity.Origin origin) {
    this.name = name;
    this.type = type;
    this.classname = className;
    this.origin = origin;
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
