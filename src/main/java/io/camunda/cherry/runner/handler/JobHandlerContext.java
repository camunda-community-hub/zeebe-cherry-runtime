/* ******************************************************************** */
/*                                                                      */
/*  JobHandlerContext                                                   */
/*                                                                      */
/*  Object given to the connector (8.3.x implementation)                */
/* ******************************************************************** */

package io.camunda.cherry.runner.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.JobContext;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.secret.SecretProvider;
import io.camunda.connector.api.validation.ValidationProvider;
import io.camunda.connector.runtime.core.secret.SecretHandler;
import io.camunda.zeebe.client.api.response.ActivatedJob;

import java.util.Objects;

/**
 * Implementation of {@link OutboundConnectorContext} passed on to
 * a {@link io.camunda.connector.api.outbound.OutboundConnectorFunction} when called from the {@link
 * CherryConnectorJobHandler}.
 */
public class JobHandlerContext implements OutboundConnectorContext {

  private final ActivatedJob job;

  final SecretProvider secretProvider;
  final ValidationProvider validationProvider;
  private final ObjectMapper objectMapper;

  private String jsonWithSecrets = null;

  private final JobContext jobContext;

  public JobHandlerContext(final ActivatedJob job,
                           final SecretProvider secretProvider,
                           final ValidationProvider validationProvider,
                           final ObjectMapper objectMapper) {

    this.job = job;
    this.secretProvider = secretProvider;
    this.validationProvider = validationProvider;
    this.objectMapper = objectMapper;
    this.jobContext = new ActivatedJobContext(job, this::getJsonReplacedWithSecrets);
  }

  @Override
  public <T> T bindVariables(Class<T> cls) {
    var mappedObject = mapJson(cls);
    validationProvider.validate(mappedObject);

    return mappedObject;
  }

  private String getJsonReplacedWithSecrets() {
    if (jsonWithSecrets == null) {

      jsonWithSecrets = getSecretHandler().replaceSecrets(job.getVariables());

    }
    return jsonWithSecrets;
  }

  private <T> T mapJson(Class<T> cls) {
    var jsonWithSecrets = getJsonReplacedWithSecrets();
    try {
      return objectMapper.readValue(jsonWithSecrets, cls);
    } catch (Exception e) {
      throw new ConnectorException("JSON_MAPPING", "Error during json mapping.");
    }
  }

  private SecretHandler getSecretHandler() {
    return new SecretHandler(secretProvider);
  }

  @Override
  public JobContext getJobContext() {
    return jobContext;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobHandlerContext that = (JobHandlerContext) o;
    return Objects.equals(job, that.job);
  }

  @Override
  public int hashCode() {
    return Objects.hash(job);
  }
}
