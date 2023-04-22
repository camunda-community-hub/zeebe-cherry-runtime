/* ******************************************************************** */
/*                                                                      */
/*  WorkerRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/runner/list                */
/*  http://localhost:8080/cherry/api/runner/c-files-load-from-disk/stop */
/* ******************************************************************** */
package io.camunda.cherry.admin;

import io.camunda.cherry.db.entity.RunnerExecutionEntity;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.IntFrameworkRunner;
import io.camunda.cherry.definition.RunnerDecorationTemplate;
import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.runner.RunnerFactory;
import io.camunda.cherry.runner.StorageRunner;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.runtime.HistoryPerformance;
import io.camunda.cherry.runtime.OperationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("cherry")
public class RunnerRestController {

  Logger logger = LoggerFactory.getLogger(RunnerRestController.class.getName());
  @Autowired
  JobRunnerFactory cherryJobRunnerFactory;

  @Autowired
  HistoryFactory historyFactory;

  @Autowired
  HistoryPerformance cherryHistoricPerformance;

  @Autowired
  RunnerFactory runnerFactory;

  @Autowired
  OperationFactory operationFactory;

  /**
   * Get list of worker. Multiple result is possibles
   * /api/runner/list?logo=true&stats=true&delaysstatsinhour=24
   *
   * @param logo   if true, logo is returned
   * @param stats  if true, execution on statistics is returned
   * @param period periodStatistic to cover
   * @return a list of information on runner
   */
  @GetMapping(value = "/api/runner/list", produces = "application/json")
  public List<RunnerInformation> getRunnersList(@RequestParam(name = "logo", required = false) Boolean logo,
                                                @RequestParam(name = "stats", required = false) Boolean stats,
                                                @RequestParam(name = "period", required = false) String period) {
    Instant instantNow = Instant.now();
    List<AbstractRunner> listRunners = getListRunners(true);
    HistoryPerformance.PeriodStatistic periodStatistic = getPeriodStatisticFromPeriod(period);

    Instant dateThreshold = cherryHistoricPerformance.getInstantThresholdFromPeriod(instantNow, periodStatistic);

    return listRunners.stream()
        .map(RunnerInformation::getRunnerInformation)
        .map(w -> this.completeRunnerInformation(w, // this
            logo == null || logo,  // logo
            stats != null && stats, // stats
            instantNow, periodStatistic))
        .toList();
  }

  @GetMapping(value = "/api/runner/dashboard", produces = "application/json")
  public Map<String, Object> getDashboard(@RequestParam(name = "period", required = false) String period,
                                          @RequestParam(name = "orderBy", required = false) String orderByParam) {
    Map<String, Object> info = new HashMap<>();

    DisplayOrderBy orderBy = DisplayOrderBy.NAMEACS;
    try {
      orderBy = DisplayOrderBy.valueOf(orderByParam.toUpperCase());
    } catch (Exception e) {
      logger.error("getDashboard: bad value for orderByParam[" + orderByParam + "]");
    }
    Instant instantNow = Instant.now();

    HistoryPerformance.PeriodStatistic periodStatistic = getPeriodStatisticFromPeriod(period);

    long totalSucceeded = 0;
    long totalFailed = 0;
    long totalBpmnError = 0;
    List<Map<String, Object>> listDetails = new ArrayList<>();
    List<AbstractRunner> listRunners = getListRunners(true);

    for (AbstractRunner runner : listRunners) {
      Map<String, Object> infoRunner = new HashMap<>();
      HistoryFactory.Statistic statisticRunner = historyFactory.getStatistic(runner.getType(), instantNow,
          periodStatistic);
      HistoryPerformance.Performance performanceRunner = historyFactory.getPerformance(runner.getType(), instantNow,
          periodStatistic);

      infoRunner.put("name", (runner.getName() == null ? "" : runner.getName()));
      infoRunner.put("type", runner.getType());
      infoRunner.put("classrunner", runner.isWorker() ? "worker" : "connector");
      infoRunner.put("collectionname", runner.getCollectionName());

      infoRunner.put("logo", runner.getLogo());
      try {
        infoRunner.put("active", cherryJobRunnerFactory.isRunnerActive(runner.getType()));
      } catch (JobRunnerFactory.OperationException e) {
        infoRunner.put("active", false);
      }
      infoRunner.put("statistic", statisticRunner);
      infoRunner.put("nbexec", statisticRunner.executions);
      infoRunner.put("nbfail", statisticRunner.executionsBpmnErrors + statisticRunner.executionsFailed);
      infoRunner.put("nboverthreshold", 0);
      infoRunner.put("performance", performanceRunner);
      listDetails.add(infoRunner);

      totalSucceeded += statisticRunner.executionsSucceeded;
      totalFailed += statisticRunner.executionsFailed;
      totalBpmnError += statisticRunner.executionsBpmnErrors;
    }
    Comparator<Map<String, Object>> orderComparator = (h1, h2) -> ((String) h1.get("name")).compareTo(
        (String) h2.get("name"));

    switch (orderBy) {
    case NAMEACS -> orderComparator = (h1, h2) -> ((String) h1.get("name")).compareTo((String) h2.get("name"));
    case NAMEDES -> orderComparator = (h1, h2) -> ((String) h2.get("name")).compareTo((String) h1.get("name"));
    case EXECASC -> orderComparator = (h1, h2) -> ((Long) h1.get("nbexec")).compareTo((Long) h2.get("nbexec"));
    case EXECDES -> orderComparator = (h1, h2) -> ((Long) h2.get("nbexec")).compareTo((Long) h1.get("nbexec"));
    case FAILASC -> orderComparator = (h1, h2) -> ((Long) h1.get("nbfail")).compareTo((Long) h2.get("nbfail"));
    case FAILDES -> orderComparator = (h1, h2) -> ((Long) h2.get("nbfail")).compareTo((Long) h1.get("nbfail"));
    }

    listDetails = listDetails.stream().sorted(orderComparator).toList();
    if (listDetails.size() > 0) {
      logger.info("RunnerRestController.orderBy[" + orderBy + "] First[" + listDetails.get(0).get("name") + "]");
    }

    info.put("details", listDetails);
    info.put("totalExecutionsSucceeded", totalSucceeded);
    info.put("totalExecutionsFailed", totalFailed);
    info.put("totalExecutionsBpmnErrors", totalBpmnError);
    info.put("totalExecutions", totalSucceeded + totalFailed + totalBpmnError);
    info.put("nbRunners", listRunners.size());
    info.put("timestamp", String.valueOf(System.currentTimeMillis()));
    return info;
  }

  @GetMapping(value = "/api/runner/detail", produces = "application/json")
  public Optional<RunnerInformation> getWorker(@RequestParam(name = "runnertype") String runnerType,
                                               @RequestParam(name = "logo", required = false) Boolean logo,
                                               @RequestParam(name = "stats", required = false) Boolean stats,
                                               @RequestParam(name = "period", required = false) String period) {
    Instant instantNow = Instant.now();
    HistoryPerformance.PeriodStatistic periodStatistic = getPeriodStatisticFromPeriod(period);
    Instant dateThreshold = cherryHistoricPerformance.getInstantThresholdFromPeriod(instantNow, periodStatistic);

    List<AbstractRunner> listRunners = getListRunners(true);

    return listRunners.stream()
        .filter(worker -> worker.getIdentification().equals(runnerType))
        .map(RunnerInformation::getRunnerInformation)
        .map(w -> this.completeRunnerInformation(w, logo == null || logo, stats != null && stats,
            // false if not asked
            instantNow, periodStatistic)) // 23 hours is not set
        .findFirst();
  }

  /**
   * Get operations for a runner. We get one week of operation
   *
   * @param runnerType        type of the runner we search the operations
   * @param nbHoursMonitoring from now to now-nbHoursMonitoring
   * @return
   */
  @GetMapping(value = "/api/runner/operations", produces = "application/json")
  public Map<String, Object> getOperation(@RequestParam(name = "runnertype") String runnerType,
                                          @RequestParam(name = "nbhoursmonitoring", required = false) Integer nbHoursMonitoring) {
    Map<String, Object> info = new HashMap<>();
    Instant instantNow = Instant.now();
    int nbHours = 24;
    if (nbHoursMonitoring != null)
      nbHours = nbHoursMonitoring.intValue();
    if (nbHours < 1)
      nbHours = 1;
    if (nbHours > 30 * 7 * 24)
      nbHours = 30 * 7 * 24;

    Instant dateThreshold = instantNow.minusSeconds(nbHours * 60 * 60);
    List<RunnerExecutionEntity> listExecutions = historyFactory.getExecutions(runnerType, instantNow, dateThreshold);

    // the errors
    List<RunnerExecutionEntity> listErrors = listExecutions.stream()
        .filter(t -> AbstractRunner.ExecutionStatusEnum.FAIL.equals(t.status)
            || AbstractRunner.ExecutionStatusEnum.BPMNERROR.equals(t.status))
        .toList();
    info.put("errors", listErrors);

    // operation

    info.put("executions", listExecutions.stream() // Stream
        .map(t -> {
          Map<String, Object> item = new HashMap<>();
          item.put("status", t.status.toString());
          item.put("executionTime", t.executionTime);
          item.put("durationms", t.executionMs);
          return item;
        }).toList());

    List<RunnerExecutionEntity> listOperations = operationFactory.getOperations(runnerType, instantNow, dateThreshold);
    info.put("operations", listOperations);

    return info;

  }

  /**
   * Ask to stop a specific worker
   *
   * @param runnerType runner to stop, by the type
   * @return NOTFOUND or the worker information on this worker
   */
  @PutMapping(value = "/api/runner/stop", produces = "application/json")
  public RunnerInformation stopWorker(@RequestParam(name = "runnertype") String runnerType) {
    logger.info("Stop requested for runnerType[" + runnerType + "]");
    try {
      boolean isStopped = cherryJobRunnerFactory.stopRunner(runnerType);
      logger.info("Stop executed for runnerType[" + runnerType + "]: " + isStopped);
      AbstractRunner runner = getRunnerByName(runnerType);
      RunnerInformation runnerInfo = RunnerInformation.getRunnerInformation(runner);
      return completeRunnerInformation(runnerInfo, false, false, null, null);
    } catch (JobRunnerFactory.OperationException e) {
      if (JobRunnerFactory.RUNNER_NOT_FOUND.equals(e.getExceptionCode()))
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + runnerType + "] not found");
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "WorkerName [" + runnerType + "] error " + e);
    }
  }

  /**
   * Ask to start a specific worker
   *
   * @param runnerName worker to start
   * @return NOTFOUND or the worker information on this worker
   */
  @PutMapping(value = "/api/runner/start", produces = "application/json")
  public RunnerInformation startWorker(@RequestParam(name = "name") String runnerName) {
    logger.info("Start requested for [" + runnerName + "]");
    try {
      boolean isStarted = cherryJobRunnerFactory.startRunner(runnerName);
      logger.info("Start executed for [" + runnerName + "]: " + isStarted);
      AbstractRunner runner = getRunnerByName(runnerName);
      RunnerInformation runnerInfo = RunnerInformation.getRunnerInformation(runner);
      return completeRunnerInformation(runnerInfo, false, false, null, null);
    } catch (JobRunnerFactory.OperationException e) {
      if (JobRunnerFactory.RUNNER_NOT_FOUND.equals(e.getExceptionCode()))
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + runnerName + "] not found");
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "WorkerName [" + runnerName + "] error " + e);
    }
  }

  /**
   * Download the Template for a runner
   *
   * @param runnerName           worker to start. If not present, all runners are part of the result
   * @param withFrameworkRunners if true, then runners from the framework are included. In general we don't want, else these runners will be present in each collection, and Modeler will throw a duplicate errors
   * @return NOTFOUND or the worker information on this worker
   */
  @GetMapping(value = "/api/runner/template", produces = "application/json")
  public String getTemplate(@RequestParam(name = "name", required = false) String runnerName,
                            @RequestParam(name = "withframeworkrunners", required = false) Boolean withFrameworkRunners) {
    boolean withFrameworkRunnersIncluded = (withFrameworkRunners != null && withFrameworkRunners);
    logger.info(
        "Download template requested for " + (runnerName == null ? "Complete collection" : "[" + runnerName + "]")
            + " FrameworkIncluded[" + withFrameworkRunnersIncluded + "]");
    if (runnerName == null) {
      // generate for ALL runners
      List<Map<String, Object>> listTemplate = getListRunners(withFrameworkRunnersIncluded).stream()
          .map(runner -> new RunnerDecorationTemplate(runner).getTemplate())
          .toList();
      return RunnerDecorationTemplate.getJsonFromList(listTemplate);
    }

    AbstractRunner runner = getRunnerByName(runnerName);
    if (runner == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + runnerName + "] not found");
    Map<String, Object> templateContent = new RunnerDecorationTemplate(runner).getTemplate();
    return RunnerDecorationTemplate.getJsonFromList(List.of(templateContent));
  }

  /**
   * @param runnerName           worker to start. If not present, all runners are part of the result
   * @param withFrameworkRunners if true, then runners from the framework are included. In general we don't want, else these runners will be present in each collection, and Modeler will throw a duplicate errors
   * @param separateTemplate     if true, the ZIP file contains one file per runner
   * @return a File to download
   * @throws IOException can't write the content to the HTTP response
   */
  @GetMapping(value = "/api/runner/templatefile", produces = MediaType.TEXT_PLAIN_VALUE)
  public @ResponseBody
  ResponseEntity downloadTemplate(@RequestParam(name = "name", required = false) String runnerName,
                                  @RequestParam(name = "withframeworkrunners", required = false) Boolean withFrameworkRunners,
                                  @RequestParam(name = "separatetemplate", required = false) Boolean separateTemplate)
      throws IOException {
    boolean withFrameworkRunnersIncluded = (withFrameworkRunners != null && withFrameworkRunners);
    // Zip file required? Add all templates in the ZIP.
    if (separateTemplate == null && withFrameworkRunners == null)
      withFrameworkRunnersIncluded = true;
    logger.info(
        "Download template requested for " + (runnerName == null ? "Complete collection" : "[" + runnerName + "]")
            + " FrameworkIncluded[" + withFrameworkRunnersIncluded + "]");
    try {

      Map<String, String> mapContent = new HashMap<>();

      String collectionName = null;
      if (runnerName != null) {
        AbstractRunner runner = getRunnerByName(runnerName);
        if (runner == null)
          throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + runnerName + "] not found");
        collectionName = runner.getName();
        mapContent.put(collectionName,
            RunnerDecorationTemplate.getJsonFromList(List.of(new RunnerDecorationTemplate(runner).getTemplate())));

      } else if (Boolean.TRUE.equals(separateTemplate) || separateTemplate == null) {
        // one file per runner
        for (AbstractRunner runner : getListRunners(withFrameworkRunnersIncluded)) {
          Map<String, Object> templateContent = new RunnerDecorationTemplate(runner).getTemplate();
          mapContent.put(runner.getName(), RunnerDecorationTemplate.getJsonFromList(List.of(templateContent)));
          if (collectionName == null)
            collectionName = runner.getCollectionName();
        }
      } else {
        // one file with all runner

        List<AbstractRunner> listRunners = getListRunners(withFrameworkRunnersIncluded);
        // generate for ALL runners
        List<Map<String, Object>> listTemplate = listRunners.stream()
            .map(runner -> new RunnerDecorationTemplate(runner).getTemplate())
            .toList();
        Optional<String> collectionNameOp = listRunners.stream().findFirst().map(AbstractRunner::getCollectionName);
        collectionName = collectionNameOp.isPresent() ? collectionNameOp.get() : "Cherry";
        mapContent.put(collectionName, RunnerDecorationTemplate.getJsonFromList(listTemplate));
      }

      // zip the result
      ZipOperation zipOperation = new ZipOperation("element-template");
      try {
        for (Map.Entry<String, String> template : mapContent.entrySet()) {
          zipOperation.addZipContent(template.getKey() + ".json", template.getValue());
        }
        zipOperation.close();
      } catch (IOException e) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Zip operation failed");
      }

      byte[] contentBytes = zipOperation.getBytes();
      HttpHeaders header = new HttpHeaders();
      header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + collectionName + "Template.zip");
      header.add("Cache-Control", "no-cache, no-store, must-revalidate");
      header.add("Pragma", "no-cache");
      header.add("Expires", "0");

      ByteArrayResource resource = new ByteArrayResource(contentBytes);

      return ResponseEntity.ok()
          .headers(header)
          .contentLength(contentBytes.length)
          .contentType(MediaType.parseMediaType("application/json"))
          .body(resource);
    } catch (Exception e) {
      logger.error(
          "Download template error for " + (runnerName == null ? "Complete collection" : "[" + runnerName + "]")
              + " FrameworkIncluded[" + withFrameworkRunnersIncluded + "] :" + e);
      return ResponseEntity.internalServerError().body(null);

    }

  }

  private AbstractRunner getRunnerByName(String runnerName) {
    List<AbstractRunner> listFiltered = runnerFactory.getAllRunners(new StorageRunner.Filter().name(runnerName));

    if (listFiltered.size() != 1)
      return null;
    return listFiltered.get(0);

  }

  private RunnerInformation completeRunnerInformation(RunnerInformation runnerInformation,
                                                      boolean withLogo,
                                                      boolean withStats,
                                                      Instant instantNow,
                                                      HistoryPerformance.PeriodStatistic periodStatistic) {
    try {
      runnerInformation.setActive(cherryJobRunnerFactory.isRunnerActive(runnerInformation.getName()));
      runnerInformation.setDisplayLogo(withLogo);

      if (withStats) {
        runnerInformation.setStatistic(
            historyFactory.getStatistic(runnerInformation.getType(), instantNow, periodStatistic));
        runnerInformation.setPerformance(
            historyFactory.getPerformance(runnerInformation.getType(), instantNow, periodStatistic));
      }

    } catch (JobRunnerFactory.OperationException e) {
      // definitively not expected
    }
    return runnerInformation;
  }

  private List<AbstractRunner> getListRunners(boolean withFrameworkRunnersIncluded) {
    // get the list of running, with the framework runner or not.
    List<AbstractRunner> listRunners = runnerFactory.getAllRunners(new StorageRunner.Filter());

    if (!withFrameworkRunnersIncluded) {
      listRunners = listRunners.stream().filter(t -> {
        return !(t instanceof IntFrameworkRunner);
      }).toList();
    }
    // order by the name
    Comparator<AbstractRunner> orderComparator = (h1, h2) -> h1.getIdentification().compareTo(h2.getIdentification());

    listRunners = listRunners.stream().sorted(orderComparator).toList();
    return listRunners;
  }

  private HistoryPerformance.PeriodStatistic getPeriodStatisticFromPeriod(String period) {
    try {
      if (period == null)
        return HistoryPerformance.PeriodStatistic.FOURHOUR;
      return HistoryPerformance.PeriodStatistic.valueOf(period);
    } catch (Exception e) {
      logger.error("Unknow PeriodStatisctic[" + period + "]");
      return HistoryPerformance.PeriodStatistic.FOURHOUR;
    }
  }

  public enum DisplayOrderBy {NAMEACS, NAMEDES, EXECASC, EXECDES, FAILASC, FAILDES}
}
