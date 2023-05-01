/* ******************************************************************** */
/*                                                                      */
/*  StoreRestController                                                 */
/*                                                                      */
/*  Service to access Secret and Env                                    */
/*  - Secret: input in the connector ask to resolve a value from a key  */
/*      Example : database: secret.database                             */
/*  - Env: Cherry Adminstrator decide to replace an Input value         */
/*      Example: database="valid@testPlatorm"                           */
/*              Administrator give the value "production@server         */
/* ******************************************************************** */
package io.camunda.cherry.secretenv;

import io.camunda.cherry.db.entity.KeyValueEntity;
import io.camunda.cherry.db.repository.KeyValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SecretEnvService {

  @Autowired
  KeyValueRepository keyValueRepository;

  @Autowired
  private Environment envSpring;

  public List<KeyValueEntity> getListKeyValue(KeyValueEntity.KeyValueType origin) {
    return keyValueRepository.getAllByOrigin(origin);
  }

  /**
   * Save (update or insert) a key value (insert or update)
   *
   * @param id       Identification of the key - id null, a new entity is created
   * @param typeKey  type of key
   * @param name     name of the key
   * @param value    value
   * @param isSecret secret or not
   */
  public KeyValueEntity saveKeyValue(Long id,
                                     KeyValueEntity.KeyValueType typeKey,
                                     String name,
                                     String value,
                                     boolean isSecret) {
    KeyValueEntity keyValueEntity = null;
    if (id != null) {
      Optional<KeyValueEntity> entity = keyValueRepository.findById(id);
      if (entity.isPresent())
        keyValueEntity = entity.get();
    }
    if (keyValueEntity == null) {
      keyValueEntity = new KeyValueEntity();
      keyValueEntity.origin = typeKey;
    }
    // name may have change
    keyValueEntity.name = name;
    keyValueEntity.value = value;
    keyValueEntity.isSecret = isSecret;
    keyValueRepository.save(keyValueEntity);
    return keyValueEntity;
  }

  public Optional<KeyValueEntity> getKeyValue(KeyValueEntity.KeyValueType typeKey, String name) {
    KeyValueEntity keyValueEntity = keyValueRepository.findByName(name, typeKey);
    if (keyValueEntity != null) {
      return Optional.of(keyValueEntity);
    }
    String value = envSpring.getProperty(name);
    if (value != null) {
      keyValueEntity = new KeyValueEntity();
      keyValueEntity.name = name;
      keyValueEntity.value = value;
      return Optional.of(keyValueEntity);

    }
    return Optional.ofNullable(null);
  }

  public void deleteKeyValue(Long id) {
    Optional<KeyValueEntity> keyValueEntity = keyValueRepository.findById(id);
    if (keyValueEntity.isEmpty()) {
      return;
    }
    keyValueRepository.delete(keyValueEntity.get());
  }

  public Object getValueFromEntity(KeyValueEntity keyValueEntity) {
    if (keyValueEntity.value == null)
      return null;
    // the key may have a function to change the type, else it's a string
    return keyValueEntity.value;

  }

}
