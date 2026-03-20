package io.camunda.cherry.admin;

import io.camunda.cherry.tenants.TenantsManager;
import io.camunda.cherry.zeebe.OrchestrationAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("cherry")
@RestController
public class TenantRestController {
    Logger logger = LoggerFactory.getLogger(TenantRestController.class.getName());

    @Autowired
    private TenantsManager tenantsManager;

    @GetMapping(value = "/api/tenants/list", produces = "application/json")
    public Map<String, Object> getOperation() {

        List<OrchestrationAPI.TenantInformation> listTenants = tenantsManager.getListTenants();

        logger.info("Tenants {}", listTenants.toString());
        Map<String, Object> operation = new HashMap<>();
        operation.put("tenants", listTenants);
        operation.put("status", tenantsManager.getErrorMessage());
        operation.put("delayRefresh", tenantsManager.getDelayRefreshInMinutes());
        return operation;
    }
}
