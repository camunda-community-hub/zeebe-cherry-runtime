/* ******************************************************************** */
/*                                                                      */
/*  SecretEnvController                                                 */
/*                                                                      */
/*  Rest controller to access the SecretEnv Service                     */
/* ******************************************************************** */
package io.camunda.cherry.secretenv;

import io.camunda.cherry.db.entity.KeyValueEntity;
import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.runner.LogOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("cherry")
public class SecretEnvRestController {

  @Autowired
  SecretEnvService secretEnvService;
  @Autowired
  LogOperation logOperation;

  @GetMapping(value = "/api/secretenv/list", produces = "application/json")
  public List<Map<String, Object>> listKeyValue(@RequestParam(name = "type", required = true) String type) {
    try {
      KeyValueEntity.KeyValueType keyValueType = KeyValueEntity.KeyValueType.valueOf(type);

      List<KeyValueEntity> listKeyValue = secretEnvService.getListKeyValue(keyValueType);
      return listKeyValue.stream().map(t -> {
        Map<String, Object> record = new HashMap<>();
        record.put("name", t.name == null ? "" : t.name);
        record.put("value", t.isSecret ? "" : t.valueKey);
        record.put("issecret", t.isSecret);
        record.put("id", String.valueOf(t.id));
        return record;
      }).sorted((e1, e2) -> e1.get("name").toString().compareTo(e2.get("name").toString())).toList();

    } catch (Exception e) {
      logOperation.log(OperationEntity.Operation.ERROR, "Can't access SecretEnv " + e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @PostMapping(value = "/api/secretenv/update", produces = "application/json")
  public Map<String, Object> updateKeyValue(@RequestParam(name = "type", required = true) String type,
                                            @RequestParam(name = "id", required = false) String id,
                                            @RequestParam(name = "name", required = true) String name,
                                            @RequestParam(name = "value", required = false) String value,
                                            @RequestParam(name = "issecret", required = true) boolean isSecret) {
    try {
      KeyValueEntity.KeyValueType keyValueType = KeyValueEntity.KeyValueType.valueOf(type);
      KeyValueEntity keyValueEntity = secretEnvService.saveKeyValue(id == null ? null : Long.valueOf(id), keyValueType,
          name, value, isSecret);
      Map<String, Object> info = new HashMap<>();
      info.put("id", keyValueEntity != null ? String.valueOf(keyValueEntity.id) : null);
      return info;
    } catch (DataIntegrityViolationException vioex) {
      Map<String, Object> info = new HashMap<>();
      info.put("error", "This name already exist");
      return info;

    } catch (Exception e) {
      logOperation.log(OperationEntity.Operation.ERROR, "Can't update an environment variable " + e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @PostMapping(value = "/api/secretenv/delete", produces = "application/json")
  public Map<String, Object> deleteKeyValue(@RequestParam(name = "id", required = true) String id) {
    try {
      secretEnvService.deleteKeyValue(Long.valueOf(id));
      Map<String, Object> info = new HashMap<>();
      info.put("id", id);
      return info;

    } catch (Exception e) {
      logOperation.log(OperationEntity.Operation.ERROR, "Can't delete an environment variable " + e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }
}
