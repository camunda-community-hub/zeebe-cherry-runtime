/* ******************************************************************** */
/*                                                                      */
/*  StoreRestController                                                 */
/*                                                                      */
/*  Rest controller to access the Store Service                         */
/* ******************************************************************** */
package io.camunda.cherry.store;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.exception.TechnicalException;
import io.camunda.cherry.runner.LogOperation;
import io.camunda.cherry.runner.RunnerFactory;
import io.camunda.cherry.runner.StorageRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("cherry")
public class StoreRestController {

  @Autowired
  StoreService storeService;

  @Autowired
  RunnerFactory runnerFactory;

  @Autowired
  LogOperation logOperation;

  @GetMapping(value = "/api/store/lastrelease", produces = "application/json")
  public String outOfTheBoxLastRelease() {
    return storeService.getLatestRelease();
  }

  @GetMapping(value = "/api/store/list", produces = "application/json")
  public List<Map<String, Object>> listConnectorInStore() {
    try {
      String lastRelease = storeService.getLatestRelease();
      Map<String, String> connectors = storeService.listConnectors(lastRelease);

      List<RunnerDefinitionEntity> listRunnersEntity = runnerFactory.getAllRunnersEntity(
          new StorageRunner.Filter().isStore(true));
      Map<String, RunnerDefinitionEntity> mapRunners = listRunnersEntity.stream()
          .collect(Collectors.toMap(x -> x.name, x -> {
            return x;
          }));

      List<Map<String, Object>> result = new ArrayList<>();
      return connectors.keySet().stream().sorted().map(t -> {
        Map<String, Object> mapConnector = new HashMap<>();
        mapConnector.put("name", t);
        RunnerDefinitionEntity runnerEntity = mapRunners.get(t);
        if (runnerEntity == null) {
          mapConnector.put("status", "NEW");
        } else if (lastRelease.equals(runnerEntity.release)) {
          mapConnector.put("status", "UPDATED");
        } else {
          mapConnector.put("status", "OLD");
        }
        mapConnector.put("currentrelease", runnerEntity == null ? "" : runnerEntity.release);
        mapConnector.put("storerelease", lastRelease);

        return mapConnector;
      }).toList();
    } catch (TechnicalException e) {
      logOperation.log(OperationEntity.Operation.ERROR, "Can't access Store " + e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @GetMapping(value = "/api/store/download", produces = "application/json")
  public Map<String, Object> download(@RequestParam(name = "name", required = false) String connectorName) {
    try {
      Map<String, Object> connectorDownloaded = new HashMap<>();

      String lastRelease = storeService.getLatestRelease();
      StoreService.ConnectorStore connectorStore = storeService.downloadConnector(connectorName, lastRelease);
      // Now, save this new connector

      return connectorDownloaded;
    } catch (TechnicalException e) {
      logOperation.log(OperationEntity.Operation.ERROR,
          "Can't download connector[" + connectorName + "] " + e.getMessage());
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }
}
