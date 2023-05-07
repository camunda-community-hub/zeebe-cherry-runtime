package io.camunda.cherry.operationlog;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.runtime.OperationFactory;
import io.camunda.cherry.util.DateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("cherry")
public class OperationLogRestController {

  Logger logger = LoggerFactory.getLogger(OperationLogRestController.class.getName());

  @Autowired
  OperationFactory operationFactory;

  @GetMapping(value = "/api/operationlog/list", produces = "application/json")
  public Map<String, Object> listOperations(@RequestParam(name = "nbhoursmonitoring", required = false) Integer nbHoursMonitoring,
                                            @RequestParam(name = "pagenumber", required = false) Integer pageNumber,
                                            @RequestParam(name = "rowsperpage", required = false) Integer rowsPerPage,
                                            @RequestParam(name = "timezoneoffset") Long timezoneOffset) {
    Map<String, Object> info = new HashMap<>();
    LocalDateTime dateNow = DateOperation.getLocalDateTimeNow();
    int nbHours;
    try {
      nbHours = Math.max(nbHoursMonitoring == null ? 24 : nbHoursMonitoring, 1);
      nbHours = Math.min(30 * 7 * 24, nbHours);
    } catch (Exception e) {
      logger.error("RunnerRestController.getOperation: value not acceptable [{}]", nbHoursMonitoring);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value not acceptable [" + nbHoursMonitoring + "]");
    }

    int pageNumberInt = pageNumber == null ? 0 : pageNumber;
    int rowsPerPageInt = rowsPerPage == null ? 20 : rowsPerPage;
    if (rowsPerPageInt < 1 || rowsPerPageInt > 10000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rowsPerPage must be between [1..10000]");

    LocalDateTime dateThreshold = DateOperation.getLocalDateTimeNow().minusHours(nbHours);

    List<OperationEntity> listOperations = operationFactory.getAllOperations(dateNow, dateThreshold);
    List<Map<String, Object>> listOperationsMap = listOperations.stream().map(t -> {
      Map<String, Object> infoOperation = new HashMap<>();
      infoOperation.put("hostname", t.hostName);
      infoOperation.put("runnerType", t.runnerType);
      infoOperation.put("executionTime", DateOperation.dateTimeToHumanString(t.executionTime, timezoneOffset));
      infoOperation.put("operation", t.operation.toString());
      infoOperation.put("message", t.message);
      return infoOperation;
    }).toList();
    info.put("operations", listOperationsMap);

    return info;
  }

}
