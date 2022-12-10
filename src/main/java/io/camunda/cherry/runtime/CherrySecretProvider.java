package io.camunda.cherry.runtime;

import io.camunda.connector.api.secret.SecretProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Configuration
public class CherrySecretProvider implements SecretProvider {

  @Autowired
  private Environment envSpring;

  @Override
  public String getSecret(String key) {
    // Different locations

    // First, in the applications
    String value = envSpring.getProperty(key);
    if (value!=null)
      return value;

    // Second, may be a Env?
    value = System.getProperty(key);
    if (value!=null)
      return value;

    // value if not find
    return null;
  }
}
