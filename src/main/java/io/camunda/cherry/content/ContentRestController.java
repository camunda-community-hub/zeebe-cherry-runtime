package io.camunda.cherry.content;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.JarDefinitionRepository;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.util.DateOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

  @GetMapping(value = "/api/content/list", produces = "application/json")
  public List<Map<String, Object>> listContent(@RequestParam(name = "timezoneoffset") Long timezoneOffset) {
    List<Map<String, Object>> listContent = new ArrayList<>();
    List<JarStorageEntity> listJarStorageEntity = jarDefinitionRepository.getAll();
    List<RunnerDefinitionEntity> listRunnerDefinition = runnerDefinitionRepository.selectAllByJarNotNull();

    for (JarStorageEntity storageEntity : listJarStorageEntity) {
      Map<String, Object> recordStorage = new HashMap<>();
      recordStorage.put("name", storageEntity.name);
      recordStorage.put("id", storageEntity.id);

      List<Map<String, Object>> listUsedBy = listRunnerDefinition.stream().filter(t -> {
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

}
