/* ******************************************************************** */
/*                                                                      */
/*  JobCountRestController                                              */
/*                                                                      */
/*  Rest API for the "Job count" page                                   */
/* ******************************************************************** */
package io.camunda.cherry.rest;

import io.camunda.cherry.db.StorageService;
import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import io.camunda.cherry.db.repository.TopicCountRepository;
import io.camunda.cherry.definition.AbstractRunner;
import io.camunda.cherry.runner.RunnerFactory;
import io.camunda.cherry.runtime.HistoryFactory;
import io.camunda.cherry.runtime.HistoryPerformance;
import io.camunda.cherry.supervisor.RunnerMonitor;
import io.camunda.cherry.supervisor.RunnerThroughput;
import io.camunda.cherry.util.DateOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("cherry")
public class JobCountRestController {

    private final RunnerFactory runnerFactory;
    private final HistoryFactory historyFactory;
    private final RunnerMonitor runnerMonitor;
    private final RunnerThroughput runnerThroughput;
    private final TopicCountRepository topicCountRepository;
    Logger logger = LoggerFactory.getLogger(JobCountRestController.class.getName());

    public JobCountRestController(RunnerFactory runnerFactory,
                                  HistoryFactory historyFactory,
                                  RunnerMonitor runnerMonitor,
                                  RunnerThroughput runnerThroughput,
                                  TopicCountRepository topicCountRepository) {
        this.runnerFactory = runnerFactory;
        this.historyFactory = historyFactory;
        this.runnerMonitor = runnerMonitor;
        this.runnerThroughput = runnerThroughput;
        this.topicCountRepository = topicCountRepository;
    }

    /**
     * ALL: every runner Cherry has a record of, including one whose jar was removed and has no
     * implementation any more (RunnerDefinitionEntity.State.NOIMPLEMENTATION).
     * ONBOARD: only runners the RunnerFactory can actually instantiate right now.
     */
    public enum Filter {ALL, ONBOARD}

    @GetMapping(value = "/api/jobcount/dashboard", produces = "application/json")
    public Map<String, Object> getJobCountDashboard(@RequestParam(name = "filter", required = false, defaultValue = "all") String filterParam,
                                                     @RequestParam(name = "period", required = false) String period,
                                                     @RequestParam(name = "timezoneoffset") Long timezoneOffset) {
        Filter filter;
        try {
            filter = Filter.valueOf(filterParam.toUpperCase());
        } catch (Exception e) {
            logger.error("getJobCountDashboard: unknown filter[{}], default to ALL", filterParam);
            filter = Filter.ALL;
        }

        LocalDateTime dateNow = DateOperation.getLocalDateTimeNow();
        HistoryPerformance.PeriodStatistic periodStatistic = getPeriodStatisticFromPeriod(period);

        List<Map<String, Object>> listDetails = new ArrayList<>();
        List<AbstractRunner> listRunners = runnerFactory.getAllRunners(new StorageService.Filter());

        for (AbstractRunner runner : listRunners) {
            HistoryPerformance.Performance performanceRunner = historyFactory.getPerformance(runner.getType(), dateNow, periodStatistic, timezoneOffset);

            Map<String, Object> infoRunner = new HashMap<>();
            infoRunner.put(RestAttribute.NAME, runner.getName() == null ? "" : runner.getName());
            infoRunner.put(RestAttribute.TYPE, runner.getType());
            infoRunner.put(RestAttribute.CONNECTOR_TYPE, runner.getType());
            infoRunner.put(RestAttribute.DESCRIPTION, runner.getDescription());
            infoRunner.put(RestAttribute.CLASS_RUNNER, runner.isWorker() ? "worker" : "connector");
            infoRunner.put(RestAttribute.LOGO, runner.getLogo());
            infoRunner.put(RestAttribute.RUNNER_STATE, runnerFactory.getRunnerState(runner.getType()).toString());
            infoRunner.put(RestAttribute.PERFORMANCE, performanceRunner);
            infoRunner.put(RestAttribute.ANALYSIS, runnerThroughput.checkPerformance(performanceRunner));
            // Not explicitly listed with the other fields, but needed to sort/display by "jobs"
            // (pending jobs waiting), which is what this page is named after.
            infoRunner.put(RestAttribute.TOPIC_COUNT, runnerMonitor.getJobCount(runner.getType()));
            listDetails.add(infoRunner);
        }

        if (filter == Filter.ALL) {
            Set<String> listRunnersTypes = listRunners.stream().map(AbstractRunner::getType).collect(Collectors.toSet());
            List<RunnerDefinitionEntity> listAllRunnersEntity = runnerFactory.getAllRunnersEntity(new StorageService.Filter());
            List<RunnerDefinitionEntity> listNoImplementation = listAllRunnersEntity.stream()
                    .filter(t -> t.state == RunnerDefinitionEntity.State.NOIMPLEMENTATION)
                    .filter(t -> !listRunnersTypes.contains(t.type))
                    .toList();
            for (RunnerDefinitionEntity noImplRunner : listNoImplementation) {
                HistoryPerformance.Performance emptyPerformance = new HistoryPerformance.Performance();

                Map<String, Object> infoRunner = new HashMap<>();
                infoRunner.put(RestAttribute.NAME, noImplRunner.name == null ? "" : noImplRunner.name);
                infoRunner.put(RestAttribute.TYPE, noImplRunner.type);
                infoRunner.put(RestAttribute.CONNECTOR_TYPE, noImplRunner.type);
                infoRunner.put(RestAttribute.DESCRIPTION, "No implementation available (jar removed)");
                infoRunner.put(RestAttribute.CLASS_RUNNER, "");
                infoRunner.put(RestAttribute.LOGO, "/img/Camunda.png");
                infoRunner.put(RestAttribute.RUNNER_STATE, RunnerDefinitionEntity.State.NOIMPLEMENTATION.toString());
                infoRunner.put(RestAttribute.PERFORMANCE, emptyPerformance);
                infoRunner.put(RestAttribute.ANALYSIS, runnerThroughput.checkPerformance(emptyPerformance));
                infoRunner.put(RestAttribute.TOPIC_COUNT, 0L);
                listDetails.add(infoRunner);
            }

            // Job types the engine actually has pending jobs for (collected by RunnerMonitor into
            // ch_topiccount) but that Cherry never onboarded at all - no RunnerDefinitionEntity in any
            // state. These are invisible to runnerFactory, so they'd otherwise never be shown anywhere.
            Set<String> knownTypes = new HashSet<>(listRunnersTypes);
            listAllRunnersEntity.forEach(t -> knownTypes.add(t.type));
            List<String> listUnregisteredTypes = topicCountRepository.findDistinctRunnerType().stream()
                    .filter(type -> type != null && !knownTypes.contains(type))
                    .toList();
            for (String unregisteredType : listUnregisteredTypes) {
                HistoryPerformance.Performance performanceRunner = historyFactory.getPerformance(unregisteredType, dateNow, periodStatistic, timezoneOffset);

                Map<String, Object> infoRunner = new HashMap<>();
                infoRunner.put(RestAttribute.NAME, unregisteredType);
                infoRunner.put(RestAttribute.TYPE, unregisteredType);
                infoRunner.put(RestAttribute.CONNECTOR_TYPE, unregisteredType);
                infoRunner.put(RestAttribute.DESCRIPTION, "Job type seen in the engine but not registered in Cherry (no connector or worker deployed for it)");
                infoRunner.put(RestAttribute.CLASS_RUNNER, "");
                infoRunner.put(RestAttribute.LOGO, "/img/Camunda.png");
                infoRunner.put(RestAttribute.RUNNER_STATE, "UNREGISTERED");
                infoRunner.put(RestAttribute.PERFORMANCE, performanceRunner);
                infoRunner.put(RestAttribute.ANALYSIS, runnerThroughput.checkPerformance(performanceRunner));
                infoRunner.put(RestAttribute.TOPIC_COUNT, runnerMonitor.getJobCount(unregisteredType));
                listDetails.add(infoRunner);
            }
        }

        Map<String, Object> info = new HashMap<>();
        info.put(RestAttribute.DETAILS, listDetails);
        info.put(RestAttribute.NB_RUNNERS, listDetails.size());
        info.put(RestAttribute.TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        logger.info("JobCountRestController./api/jobcount/dashboard: filter[{}] runners: {}", filter, listDetails.size());
        return info;
    }

    private HistoryPerformance.PeriodStatistic getPeriodStatisticFromPeriod(String period) {
        try {
            if (period == null)
                return HistoryPerformance.PeriodStatistic.FOURHOUR;
            return HistoryPerformance.PeriodStatistic.valueOf(period);
        } catch (Exception e) {
            logger.error("Unknow PeriodStatistic[{}]", period);
            return HistoryPerformance.PeriodStatistic.FOURHOUR;
        }
    }
}
