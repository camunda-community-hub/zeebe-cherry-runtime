package io.camunda.cherry.content;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.JarDefinitionRepository;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.exception.OperationException;
import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.runner.RunnerAdminOperation;
import io.camunda.cherry.runner.RunnerFactory;
import io.camunda.cherry.runner.RunnerLightDefinition;
import io.camunda.cherry.util.DateOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("cherry")
public class ContentRestController {

  JarDefinitionRepository jarDefinitionRepository;
  RunnerDefinitionRepository runnerDefinitionRepository;
  RunnerAdminOperation runnerAdminOperation;
  RunnerFactory runnerFactory;
  JobRunnerFactory jobRunnerFactory;
  @Autowired
  JobRunnerFactory cherryJobRunnerFactory;

  public ContentRestController(JarDefinitionRepository jarDefinitionRepository,
                               RunnerDefinitionRepository runnerDefinitionRepository,
                               RunnerAdminOperation runnerAdminOperation,
                               RunnerFactory runnerFactory,
                               JobRunnerFactory jobRunnerFactory) {
    this.jarDefinitionRepository = jarDefinitionRepository;
    this.runnerDefinitionRepository = runnerDefinitionRepository;
    this.runnerAdminOperation = runnerAdminOperation;
    this.runnerFactory = runnerFactory;
    this.jobRunnerFactory = jobRunnerFactory;
  }

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
        recordRunner.put("collectionName", t.collectionName);
        recordRunner.put("activeRunner", cherryJobRunnerFactory.isActiveRunner(t.type));
        return recordRunner;
      }).toList();
      recordStorage.put("usedby", listUsedBy);
      recordStorage.put("loadedtime", DateOperation.dateTimeToHumanString(storageEntity.loadedTime, timezoneOffset));
      listContent.add(recordStorage);
    }
    List<Map<String, Object>> sortedList = listContent.stream()
        .sorted(Comparator.comparing(map -> (String) map.get("name")))
        .collect(Collectors.toList());
    return sortedList;
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
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
          "storageEntityId [" + storageEntityId + "] error " + e);

    }
    return status;

  }

  @PostMapping(value = "/api/content/add", consumes = {
      MediaType.MULTIPART_FORM_DATA_VALUE }, produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> upload(@RequestPart("File") List<MultipartFile> uploadedfiles) {
    Map<String, Object> status = new HashMap<>();

    List<String> resultLoad = new ArrayList<>();
    Map<String, String> analysisPerRunner = new HashMap<>();
    for (MultipartFile file : uploadedfiles) {
      String resultFile = "Load [" + file.getName() + "]";

      // is this worker is running?
      String jarFileName = file.getOriginalFilename();
      List<RunnerLightDefinition> listRunnerLightDefinitions = runnerFactory.saveFromMultiPartFile(file, jarFileName);

      // Now, stop all the runners containing in the jar
      Map<String, Boolean> runnerIsRunningBefore = new HashMap<>();
      for (RunnerLightDefinition runner : listRunnerLightDefinitions) {
        String analysis = "runner [" + runner.getType() + "]: ";
        if (jobRunnerFactory.isRunnerExist(runner.getType())) {
          runnerIsRunningBefore.put(runner.getType(), jobRunnerFactory.isActiveRunner(runner.getType()));
          analysis += jobRunnerFactory.isActiveRunner(runner.getType()) ? "ACTIVE, " : "STOPPED, ";
        } else {
          analysis += "New runner,";
        }
        if (jobRunnerFactory.isActiveRunner(runner.getType())) {
          try {
            boolean isStopped = jobRunnerFactory.stopRunner(runner.getType());
            analysis += "Stopped, ";
          } catch (OperationException operationException) {
            // we have a problem here, the copy will failed...
            analysis += "Can't stop it " + operationException.getHumanInformation() + ", ";
          }
        }
        analysisPerRunner.put(runner.getType(), analysis);
      }
      // Now, copy the JarFile in the ClassLoader factory
      runnerFactory.jarFileToClassLoader(jarFileName);

      // Now, start all runners
      for (RunnerLightDefinition runner : listRunnerLightDefinitions) {
        {
          String analysis = analysisPerRunner.getOrDefault(runner.getType(), "");

          // if the runner is new, we start it
          boolean restart = runnerIsRunningBefore.getOrDefault(runner.getType(), Boolean.TRUE);
          if (restart) {
            try {
              analysis += "Start,";

              jobRunnerFactory.startRunner(runner.getType());
            } catch (OperationException operationException) {
              analysis += "Can't Start it " + operationException.getHumanInformation() + ", ";
            }
          }
          analysisPerRunner.put(runner.getType(), analysis);
        }
        resultFile += analysisPerRunner.values().stream().collect(Collectors.joining(","));
        resultLoad.add(resultFile);
      }
    }

    status.put("status", "OK");
    status.put("resultLoad", resultLoad);
    return status;
  }

  /*
  @Bean(name = MultipartFilter.DEFAULT_MULTIPART_RESOLVER_BEAN_NAME)
  protected MultipartResolver getMultipartResolver() {
    CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
    multipartResolver.setMaxUploadSize(20971520);
    multipartResolver.setMaxInMemorySize(20971520);
    return multipartResolver;
  }
  */

}
