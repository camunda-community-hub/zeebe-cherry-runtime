package io.camunda.cherry.tenants;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.runner.LogOperation;
import io.camunda.cherry.zeebe.OrchestrationAPI;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Configuration
public class TenantsManager {
    Logger logger = LoggerFactory.getLogger(TenantsManager.class.getName());


    @Autowired
    JobRunnerFactory jobRunnerFactory;
    @Autowired
    OrchestrationAPI orchestrationAPI;
    @Value("#{'${cherry.tenants.activeIds:}'.split(',')}")
    private Set<String> activeTenantIds;
    @Value("${cherry.tenants.refreshTenantsInMinutes:1}")
    private Integer refreshTenantsInMinutes;
    private List<OrchestrationAPI.TenantInformation> currentTenants = new ArrayList<>();
    private final String errorMessage = "";
    @Autowired
    private LogOperation logOperation;
    @Autowired
    private TaskScheduler scheduler;

    public void refreshListTenants() {

        try {
            logger.debug("start Refreshing list tenants");

            currentTenants = orchestrationAPI.getListTenants();
            for (OrchestrationAPI.TenantInformation tenant : currentTenants) {
                if (activeTenantIds.isEmpty())
                    tenant.active = true;
                else
                    tenant.active = activeTenantIds.contains(tenant.tenantId);
            }
            logger.info("Refreshing list tenants {}", currentTenants.stream()
                    .map(t -> t.tenantId + "-" + t.name + "-active? " + t.active).toList());

            // now we compare this list with the one that jobRunner has. If something change, we have to update jobRunner and ask for a restart
            List<OrchestrationAPI.TenantInformation> runnerListTenants = Optional.ofNullable(jobRunnerFactory.getListTenants()).orElse(Collections.emptyList());


            Set<String> setJobRunner = runnerListTenants.stream().map(t -> t.tenantId).collect(Collectors.toSet());
            Set<String> setTenants = currentTenants.stream()
                    .filter(t -> t.active)
                    .map(t -> t.tenantId)
                    .collect(Collectors.toSet());

            // does the list change since the last execution?
            if (setJobRunner.equals(setTenants)) {
                logger.debug("TenantsManager: Same list of Active  tenants {}", setTenants);
                return;
            }
            logOperation.log(OperationEntity.Operation.TENANTUPDATE, "Tenants list" +
                    currentTenants.stream()
                            .filter(t -> t.active)
                            .map(t -> t.tenantId + ":" + t.name)
                            .toList());
            // Refresh and restart Zeebe with this new tenants list
            // send only the active tenants
            jobRunnerFactory.setListTenants(currentTenants.stream().filter(t -> t.active).collect(Collectors.toList()));

            jobRunnerFactory.restartRunners();

        } catch (Exception e) {
            logger.error("TenantsManager: during HeartBeat ", e.getMessage());
        } finally {
            scheduler.schedule(this::refreshListTenants, Instant.now().plusSeconds(this.refreshTenantsInMinutes * 60));
        }
    }

    public boolean isTenantsActive() {
        return currentTenants.size() > 1;
    }

    public List<OrchestrationAPI.TenantInformation> getListTenants() {
        return currentTenants;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getDelayRefreshInMinutes() {
        return refreshTenantsInMinutes;
    }

    public Set<String> getActiveTenantsIds() {
        return activeTenantIds;
    }

    @PostConstruct
    public void init() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        refreshListTenants();
        scheduleNext();
    }


    private void scheduleNext() {
        scheduler.schedule(this::refreshListTenants, Instant.now().plusSeconds(this.refreshTenantsInMinutes * 20));
    }


}
