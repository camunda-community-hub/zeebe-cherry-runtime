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

}
