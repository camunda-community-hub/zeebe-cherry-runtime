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

  public String type;

  public String name;
  public RunnerDefinitionEntity.Origin origin;

  public String getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public RunnerDefinitionEntity.Origin getOrigin() {
    return origin;
  }
}
