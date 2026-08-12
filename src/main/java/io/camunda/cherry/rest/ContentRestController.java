package io.camunda.cherry.rest;

import io.camunda.cherry.db.entity.JarStorageEntity;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.JarStorageEntityRepository;
import io.camunda.cherry.db.repository.RunnerDefinitionRepository;
import io.camunda.cherry.exception.OperationException;
import io.camunda.cherry.runner.*;
import io.camunda.cherry.supervisor.Installer;
import io.camunda.cherry.util.DateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("cherry")
public class ContentRestController {
    private final JarStorageEntityRepository jarStorageEntityRepository;
    private final RunnerDefinitionRepository runnerDefinitionRepository;
    private final RunnerAdminOperation runnerAdminOperation;
    private final RunnerFactory runnerFactory;
    private final JobRunnerFactory jobRunnerFactory;
    private final RunnerUploadFactory runnerUploadFactory;
    private final Installer installer;
    Logger logger = LoggerFactory.getLogger(ContentRestController.class.getName());

    public ContentRestController(JarStorageEntityRepository jarStorageEntityRepository,
                                 RunnerDefinitionRepository runnerDefinitionRepository,
                                 RunnerAdminOperation runnerAdminOperation,
                                 RunnerFactory runnerFactory,
                                 JobRunnerFactory jobRunnerFactory,
                                 Installer installer,
                                 RunnerUploadFactory runnerUploadFactory) {
        this.jarStorageEntityRepository = jarStorageEntityRepository;
        this.runnerDefinitionRepository = runnerDefinitionRepository;
        this.runnerAdminOperation = runnerAdminOperation;
        this.runnerFactory = runnerFactory;
        this.jobRunnerFactory = jobRunnerFactory;
        this.installer = installer;
        this.runnerUploadFactory = runnerUploadFactory;
    }

    @GetMapping(value = "/api/content/list", produces = "application/json")
    public List<Map<String, Object>> listContent(@RequestParam(name = "timezoneoffset") Long timezoneOffset) {
        List<Map<String, Object>> listContent = new ArrayList<>();
        List<JarStorageEntity> listJarStorageEntity = jarStorageEntityRepository.getAll();
        List<RunnerDefinitionEntity> listRunnersDefinition = runnerDefinitionRepository.selectAllByJarNotNull();

        for (JarStorageEntity storageEntity : listJarStorageEntity) {
            Map<String, Object> recordStorage = new HashMap<>();
            recordStorage.put(RestAttribute.NAME, storageEntity.name);
            recordStorage.put(RestAttribute.STORAGE_ENTITY_ID, storageEntity.id);

            List<Map<String, Object>> listUsedBy = listRunnersDefinition.stream().filter(t -> {
                return t.jar.id.equals(storageEntity.id);
            }).map(t -> {
                Map<String, Object> recordRunner = new HashMap<>();
                recordRunner.put(RestAttribute.NAME, t.name);
                recordRunner.put(RestAttribute.COLLECTION_NAME, t.collectionName);
                recordRunner.put(RestAttribute.ACTIVE_RUNNER, jobRunnerFactory.isActiveRunner(t.type));
                return recordRunner;
            }).toList();
            recordStorage.put(RestAttribute.USED_BY, listUsedBy);
            recordStorage.put(RestAttribute.LOADED_TIME, DateOperation.dateTimeToHumanString(storageEntity.loadedTime, timezoneOffset));
            listContent.add(recordStorage);
        }
        List<Map<String, Object>> sortedList = listContent.stream()
                .sorted(Comparator.comparing(map -> (String) map.get(RestAttribute.NAME)))
                .collect(Collectors.toList());
        return sortedList;
    }

    @PutMapping(value = "/api/content/delete", produces = "application/json")
    public Map<String, Object> listContent(@RequestParam(name = "timezoneoffset") Long timezoneOffset,
                                           @RequestParam(name = "storageentityid") String storageEntityId) {

        Map<String, Object> status = new HashMap<>();
        try {
            runnerAdminOperation.deleteJarFile(Long.valueOf(storageEntityId));
            status.put(RestAttribute.STATUS, "OK");
        } catch (OperationException e) {
            if (JobRunnerFactory.RUNNER_NOT_FOUND.equals(e.getExceptionCode()))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "storageEntityId [" + storageEntityId + "] not found");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "storageEntityId [" + storageEntityId + "] error " + e);
        }
        return status;
    }

    @PostMapping(value = "/api/content/add", consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> upload(@RequestPart("File") List<MultipartFile> uploadedfiles) {
        Map<String, Object> status = new HashMap<>();

        List<String> resultLoad = new ArrayList<>();
        Map<String, String> analysisPerRunner = new HashMap<>();
        for (MultipartFile file : uploadedfiles) {
            String resultFile = "Load [" + file.getName() + "]";

            String jarFileName = file.getOriginalFilename();
            List<RunnerLightDefinition> listRunnerLightDefinitions = saveFromMultiPartFile(file, jarFileName);

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
                        analysis += "Can't stop it " + operationException.getHumanInformation() + ", ";
                    }
                }
                analysisPerRunner.put(runner.getType(), analysis);
            }

            for (RunnerLightDefinition runner : listRunnerLightDefinitions) {
                {
                    String analysis = analysisPerRunner.getOrDefault(runner.getType(), "");

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

        status.put(RestAttribute.STATUS, "OK");
        status.put(RestAttribute.RESULT_LOAD, resultLoad);
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

    /**
     * Save a new Jar file. A Jar file contains multiple runners. This method does not stop/restart runners.
     * The method save in the storage and in the classloader path. The load in the JavaClassLoader is not under
     * the responsability of the method, just to place the jar in the storage and the classloader.
     *
     * @param file multipart file
     * @return list of runners detected in the jar file
     */
    public List<RunnerLightDefinition> saveFromMultiPartFile(MultipartFile file, String jarFileName) {

        List<RunnerLightDefinition> runners = new ArrayList<>();
        try {
            ByteArrayInputStream jarFileInputStream = new ByteArrayInputStream(file.getBytes());
            runners = installer.installStartJar(jarFileName, jarFileInputStream, null);
            for (RunnerLightDefinition runner : runners) {
                try {
                    jobRunnerFactory.stopRunner(runner.getType());
                    jobRunnerFactory.startRunner(runner.getType());
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }
/*
   jarTemp = Files.createTempFile(jarFileName, ".jar");
            // Open an OutputStream to the temporary file
            outputStream = new FileOutputStream(jarTemp.toFile());
            // Transfer data from InputStream to OutputStream
            byte[] buffer = new byte[1024 * 100]; // 100Ko
            int bytesRead;
            int count = 0;
            InputStream inputStream = file.getInputStream();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                count += bytesRead;
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            outputStream = null;
 */


        } catch (Exception e) {
            logger.error("saveFromMultiPartFile: error processing [{}]: {}", jarFileName, e.getMessage());
        }
        return runners;
    }
}
