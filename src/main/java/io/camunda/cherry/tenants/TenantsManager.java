package io.camunda.cherry.tenants;

import io.camunda.cherry.db.entity.OperationEntity;
import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.runner.LogOperation;
import io.camunda.cherry.zeebe.OperateContainer;
import io.camunda.cherry.zeebe.ZeebeContainer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class TenantsManager {
    Logger logger = LoggerFactory.getLogger(TenantsManager.class.getName());

    @Autowired
    OperateContainer operateContainer;

    @Autowired
    ZeebeContainer zeebeContainer;
    @Autowired
    JobRunnerFactory jobRunnerFactory;
    @Value("${cherry.tenants.automaticDetection:false}")
    private Boolean automaticDetection;
    @Value("${cherry.tenants.tenant-ids:}")
    private List<String> listConfigurationTenants;
    @Value("${cherry.tenants.refreshTenantsInMinutes:1}")
    private Integer refreshTenantsInMinutes;
    private final boolean fixedListFromDatabase = false;

    private Set<String> setCurrentTenants = new HashSet<>();

    private boolean initialized = false;
    @Autowired
    private LogOperation logOperation;
    @Autowired
    private TaskScheduler scheduler;

    public void refreshListTenants() {
        if (!automaticDetection) {
            logger.debug("TenantsManager: Automatic detection is disabled");
            return;
        }

        try {
            if (!initialized) {
                logger.debug("TenantsManager:Initialization in progress");
                return;
            }

            if (listConfigurationTenants != null && !listConfigurationTenants.isEmpty()) {
                // a list is hard coded on this instance: do nothing
                logger.debug("TenantsManager:List of tenants fixed in the configuration");
                return;
            }
            if (fixedListFromDatabase) {
                logger.debug("TenantsManager: List is fixed in the database (administrator configuration)");
                return;
            }
            // now, we ask the engine for an automatic detection
            Set<String> currentTenants = operateContainer.getListTenants();
            // does the list change since the last execution?
            if (currentTenants.equals(setCurrentTenants)) {
                logger.debug("TenantsManager: Same list of tenants {}", setCurrentTenants);
                return;
            }
            logOperation.log(OperationEntity.Operation.TENANTUPDATE, "Tenants list[" + currentTenants + "]");

            // Refresh and restart Zeebe with this new tenants list
            setCurrentTenants = currentTenants;
            jobRunnerFactory.setListTenants(new ArrayList<>(setCurrentTenants));
            jobRunnerFactory.restartRunners();

        } catch (Exception e) {
            logger.error("TenantsManager: during HeartBeat ", e.getMessage());
        } finally {
            scheduler.schedule(this::refreshListTenants, Instant.now().plusSeconds(this.refreshTenantsInMinutes * 30));
        }
    }

    public boolean isTenantsActive() {
        return !setCurrentTenants.isEmpty();
    }

    public Set<String> getListTenants() {
        return setCurrentTenants;
    }

    @PostConstruct
    public void init() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // get the list of tenants to run. It ùay come the database, or come a specific configuration.
        if (automaticDetection) {
            // ask the current list
            setCurrentTenants = operateContainer.getListTenants();
            // Update the database with that list

        } else {
            // get the information from the database: is the list is fixed?
            // initialize fixedListFromDatabase and setCurrentTenants

        }
        // set the tenants now
        jobRunnerFactory.setListTenants(new ArrayList<>(setCurrentTenants));
        initialized = true;
        scheduleNext();
    }


    private void scheduleNext() {
        scheduler.schedule(this::refreshListTenants, Instant.now().plusSeconds(this.refreshTenantsInMinutes * 20));
    }


}
