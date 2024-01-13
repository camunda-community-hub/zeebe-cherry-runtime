package io.camunda.cherry.content;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.JarDefinitionRepository;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.exception.OperationException;
import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.runner.RunnerAdminOperation;
import io.camunda.cherry.util.DateOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("cherry")
public class ContentRestController {

  @Autowired
  JarDefinitionRepository jarDefinitionRepository;

  @Autowired
  RunnerDefinitionRepository runnerDefinitionRepository;

  @Autowired
  RunnerAdminOperation runnerAdminOperation;

  @GetMapping(value = "/api/content/list", produces = "application/json")
  public List<Map<String, Object>> listContent(@RequestParam(name = "timezoneoffset") Long timezoneOffset) {
    List<Map<String, Object>> listContent = new ArrayList<>();
    List<JarStorageEntity> listJarStorageEntity = jarDefinitionRepository.getAll();
    List<RunnerDefinitionEntity> listRunnersDefinition = runnerDefinitionRepository.selectAllByJarNotNull();

    for (JarStorageEntity storageEntity : listJarStorageEntity) {
      Map<String, Object> recordStorage = new HashMap<>();
      recordStorage.put("name", storageEntity.name);
      recordStorage.put("storageentityid", storageEntity.id);

      List<Map<String, Object>> listUsedBy = listRunnersDefinition.stream().filter(t -> {
        return t.jar.id.equals(storageEntity.id);
      }).map(t -> {
        Map<String, Object> recordRunner = new HashMap<>();
        recordRunner.put("name", t.name);
        recordRunner.put("collectionname", t.collectionName);
        return recordRunner;
      }).toList();
      recordStorage.put("usedby", listUsedBy);
      recordStorage.put("loadedtime", DateOperation.dateTimeToHumanString(storageEntity.loadedTime, timezoneOffset));
      listContent.add(recordStorage);
    }
    return listContent;
  }

  @PutMapping(value = "/api/content/delete", produces = "application/json")
  public Map<String, Object> listContent(@RequestParam(name = "timezoneoffset") Long timezoneOffset,
                                         @RequestParam(name = "storageentityid") String storageEntityId) {

    Map<String, Object> status = new HashMap<>();
    try {
      runnerAdminOperation.deleteJarFile(Long.valueOf(storageEntityId));
      status.put("status", "OK");
    } catch (OperationException e) {
      if (JobRunnerFactory.RUNNER_NOT_FOUND.equals(e.getExceptionCode()))
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "storageEntityId [" + storageEntityId + "] not found");
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "storageEntityId [" + storageEntityId + "] error " + e);

    }
    return status;

  }

}
