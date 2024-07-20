/* ******************************************************************** */
/*                                                                      */
/*  CherryMain                                                          */
/*                                                                      */
/*  main class to pilot the activity - init, refresh every 10 mn..      */
/* ******************************************************************** */
package io.camunda.cherry.runtime;

import io.camunda.cherry.runner.JobRunnerFactory;
import io.camunda.cherry.runner.RunnerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class CherryMain {

  Logger logger = LoggerFactory.getLogger(CherryMain.class.getName());

  @Autowired
  RunnerFactory runnerFactory;

  @Autowired
  JobRunnerFactory jobRunnerFactory;

  @PostConstruct
  public void init() {
    // first, check all internal runner
    logger.info("----- CherryMain.1 Load all embedded runners");
    runnerFactory.init();

    logger.info("----- CherryMain.2 purge non existing anymore runners");
    runnerFactory.synchronize();

    // at this point, the table is up-to-date, class loader is correct : let's start all runners
    logger.info("----- CherryMain.3 Start all runners");
    jobRunnerFactory.startAll();
  }

  @PreDestroy
  public void end() {
    logger.info("----- End is called");

    jobRunnerFactory.stopAll();

  }

}
