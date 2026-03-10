package io.camunda.cherry.zeebe;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.CamundaOperateClientConfiguration;
import io.camunda.operate.model.ProcessDefinition;
import io.camunda.operate.model.SearchResult;
import io.camunda.operate.search.ProcessDefinitionFilter;
import io.camunda.operate.search.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Visit https://github.com/camunda-community-hub/camunda-operate-client-java
 */
@Component
public class OperateContainer {
    Logger logger = LoggerFactory.getLogger(OperateContainer.class.getName());

    @Autowired
    CamundaOperateClient camundaOperateClient;
    CamundaOperateClientConfiguration operateConfiguration;

    public OperateContainer(CamundaOperateClientConfiguration operateConfiguration) {
        this.operateConfiguration = operateConfiguration;
    }

    public Set<String> getListTenants() {
        //Search process definitions
        ProcessDefinitionFilter processDefinitionFilter = ProcessDefinitionFilter.builder().build();
        try {
            SearchQuery searchProcessDefinitionBuilder = new SearchQuery.Builder()
                    .size(1000)
                    .build();

            SearchResult<ProcessDefinition> result = camundaOperateClient.searchProcessDefinitionResults(searchProcessDefinitionBuilder);
            Set<String> setTenantIds = result.getItems().stream().map(t -> t.getTenantId()).collect(Collectors.toSet());
            return setTenantIds;
        } catch (Exception e) {
            logger.error("Can't get listTenantsId from OperateConfiguration [{}] : {}",
                    operateConfiguration != null ? operateConfiguration.toString() : "", e.getMessage());
            // don't return an error, maybe operate is not accessible, so return an empty list
            return Collections.emptySet();
        }
    }


}

