/* ******************************************************************** */
/*                                                                      */
/*  WorkerRestController                                                 */
/*                                                                      */
/*  Rest API for the admin application                                  */
/* example: http://localhost:8080/cherry/api/runner/list                */
/*  http://localhost:8080/cherry/api/runner/c-files-load-from-disk/stop */
/* ******************************************************************** */
package io.camunda.cherry.admin;

import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.definition.IntFrameworkRunner;
import io.camunda.cherry.definition.RunnerDecorationTemplate;
import io.camunda.cherry.runtime.CherryHistoricFactory;
import io.camunda.cherry.runtime.CherryJobRunnerFactory;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("cherry")
public class RunnerRestController {

  Logger logger = LoggerFactory.getLogger(RunnerRestController.class.getName());
  @Autowired
  CherryJobRunnerFactory cherryJobRunnerFactory;

  @Autowired
  CherryHistoricFactory cherryHistoricFactory;

  /**
   * Spring populate the list of all runners
   */
  @Autowired
  private List<AbstractRunner> listRunners;

  /**
   * Spring populate this list with runner marked at Framework
   */
  @Autowired
  private List<IntFrameworkRunner> listFrameworkRunners;

  /**
   * Get list of worker. Multiple result is possibles
   * /api/runner/list?logo=true&stats=true&delaysstatsinhour=24
   *
   * @param logo              if true, logo is returned
   * @param stats             if true, execution on statistics is returned
   * @param delayStatsInHours give the delay in hour to collect data
   * @return a list of information on runner
   */
  @GetMapping(value = "/api/runner/list", produces = "application/json")
  public List<RunnerInformation> getWorkersList(@RequestParam(name = "logo", required = false) Boolean logo,
                                                @RequestParam(name = "stats", required = false) Boolean stats,
                                                @RequestParam(name = "delaystatsinhours", required = false) Integer delayStatsInHours) {

    return listRunners.stream().map(RunnerInformation::getRunnerInformation).map(w -> {
      return this.completeRunnerInformation(w, logo == null || logo, stats == null ? false : stats,
          delayStatsInHours == null ? Integer.valueOf(24) : delayStatsInHours);
    }).toList();
  }

  @GetMapping(value = "/api/runner/dashboard", produces = "application/json")
  public Map<String,Object> getDashbboard( @RequestParam(name = "delaystatsinhours", required = false) Integer delayStatsInHours) {
    Map<String,Object> info=new HashMap<>();
    int delayStatsInHoursInt = delayStatsInHours==null? 24: delayStatsInHours;

  long totalSucceed=0;
    long totalFailed=0;
    List<Map<String,Object>> listDetails = new ArrayList<>();
    for (AbstractRunner runner: listRunners ) {
      Map<String,Object> infoRunner = new HashMap<>();
      CherryHistoricFactory.Statistic statisticRunner = cherryHistoricFactory.getStatistic(runner.getName(), delayStatsInHoursInt);
      infoRunner.put("name", runner.getName());
      infoRunner.put("logo", runner.getLogo() );
      try {
        infoRunner.put("active", cherryJobRunnerFactory.isRunnerActive(runner.getName()));
      } catch (CherryJobRunnerFactory.OperationException e) {
        infoRunner.put("active", false);
      }
      infoRunner.put("statistic", statisticRunner);
      infoRunner.put("performance", cherryHistoricFactory.getPerformance(runner.getName(), delayStatsInHoursInt));
      listDetails.add( infoRunner);
      totalSucceed += statisticRunner.succeed;
      totalFailed += statisticRunner.failed;
    }
    info.put("details", listDetails);
    info.put("totalsucceed",totalSucceed);
    info.put("totalfailed",totalFailed);
    info.put("nbRunners",listRunners.size());
    return info;
  }
  @GetMapping(value = "/api/runner/detail", produces = "application/json")
  public Optional<RunnerInformation> getWorker(@RequestParam(name = "name") String runnerName,
                                               @RequestParam(name = "logo", required = false) Boolean logo,
                                               @RequestParam(name = "stats", required = false) Boolean stats,
                                               @RequestParam(name = "delaystatsinhours", required = false) Integer delayStatsInHours) {
    return listRunners.stream()
        .filter(worker -> worker.getIdentification().equals(runnerName))
        .map(RunnerInformation::getRunnerInformation)
        .map(w -> this.completeRunnerInformation(w, logo == null || logo, stats == null ? false : stats,
            delayStatsInHours))
        .findFirst();
  }

  /**
   * Ask to stop a specific worker
   *
   * @param runnerName worker to stop
   * @return NOTFOUND or the worker information on this worker
   */
  @PutMapping(value = "/api/runner/stop", produces = "application/json")
  public RunnerInformation stopWorker(@RequestParam(name = "name") String runnerName) {
    logger.info("Stop requested for [" + runnerName + "]");
    try {
      boolean isStopped = cherryJobRunnerFactory.stopRunner(runnerName);
      logger.info("Stop executed for [" + runnerName + "]: " + isStopped);
      AbstractRunner runner = getRunnerByName(runnerName);
      RunnerInformation runnerInfo = RunnerInformation.getRunnerInformation(runner);
      return completeRunnerInformation(runnerInfo, false, false, null);
    } catch (CherryJobRunnerFactory.OperationException e) {
      if (CherryJobRunnerFactory.RUNNER_NOT_FOUND.equals(e.getExceptionCode()))
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "WorkerName [" + runnerName + "] not found");
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "WorkerName [" + runnerName + "] error " + e);
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
      return completeRunnerInformation(runnerInfo, false, false, null);
    } catch (CherryJobRunnerFactory.OperationException e) {
      if (CherryJobRunnerFactory.RUNNER_NOT_FOUND.equals(e.getExceptionCode()))
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
    if (separateTemplate==null && withFrameworkRunners==null)
      withFrameworkRunnersIncluded=true;
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
          if (collectionName==null)
              collectionName=runner.getCollectionName();
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
          zipOperation.addZipContent(template.getKey()+".json", template.getValue());
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
    List<AbstractRunner> listFiltered = listRunners.stream()
        .filter(w -> w.getIdentification().equals(runnerName))
        .toList();
    if (listFiltered.size() != 1)
      return null;
    return listFiltered.get(0);

  }

  private RunnerInformation completeRunnerInformation(RunnerInformation runnerInformation,
                                                      boolean withLogo,
                                                      boolean withStats,
                                                      Integer delayStatInHour) {
    try {
      runnerInformation.setActive(cherryJobRunnerFactory.isRunnerActive(runnerInformation.getName()));
      runnerInformation.setDisplayLogo(withLogo);

      if (withStats) {
        runnerInformation.setStatistic(
            cherryHistoricFactory.getStatistic(runnerInformation.getName(), delayStatInHour));
        runnerInformation.setPerformance(
            cherryHistoricFactory.getPerformance(runnerInformation.getName(), delayStatInHour));
      }

    } catch (CherryJobRunnerFactory.OperationException e) {
      // definitively not expected
    }
    return runnerInformation;
  }

  private List<AbstractRunner> getListRunners(boolean withFrameworkRunnersIncluded) {
    // get the list of running, with the framework runner or not.

    // If runners contains ONLY framework runners, then this application is the Cherry framework, and we will return it
    if (listRunners.size() == listFrameworkRunners.size())
      return listRunners;

    if (withFrameworkRunnersIncluded)
      return listRunners;

    // listRunnersWithoutFramework
    return listRunners.stream().filter(runner -> !listFrameworkRunners.contains(runner)).toList();
  }

}
