/* ******************************************************************** */
/*                                                                      */
/*  ActivatedJobContext                                                 */
/*                                                                      */
/*  Object given to the connector (8.3.x implementation),               */
/*  kid of JobHandlerContext                                            */
/* ******************************************************************** */

package io.camunda.cherry.runner.handler;

import io.camunda.connector.api.outbound.JobContext;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import java.util.Map;
import java.util.function.Supplier;

public class ActivatedJobContext implements JobContext {

  private final ActivatedJob activatedJob;
  private final Supplier<String> variables;

  public ActivatedJobContext(ActivatedJob activatedJob, Supplier<String> variables) {
    this.activatedJob = activatedJob;
    this.variables = variables;
  }

  @Override
  public Map<String, String> getCustomHeaders() {
    return activatedJob.getCustomHeaders();
  }

  @Override
  public String getVariables() {
    return variables.get();
  }

  @Override
  public String getType() {
    return activatedJob.getType();
  }

  @Override
  public long getProcessInstanceKey() {
    return activatedJob.getProcessInstanceKey();
  }

  @Override
  public String getBpmnProcessId() {
    return activatedJob.getBpmnProcessId();
  }

  @Override
  public int getProcessDefinitionVersion() {
    return activatedJob.getProcessDefinitionVersion();
  }

  @Override
  public long getProcessDefinitionKey() {
    return activatedJob.getProcessDefinitionKey();
  }

  @Override
  public String getElementId() {
    return activatedJob.getElementId();
  }

  @Override
  public long getElementInstanceKey() {
    return activatedJob.getElementInstanceKey();
  }

  @Override
  public String getTenantId() {
    return activatedJob.getTenantId();
  }
}
