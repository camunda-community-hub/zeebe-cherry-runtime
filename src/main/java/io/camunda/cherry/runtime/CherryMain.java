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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class CherryMain {

  Logger logger = LoggerFactory.getLogger(CherryMain.class.getName());

  @Autowired
  RunnerFactory runnerFactory;

  @Autowired
  JobRunnerFactory cherryJobRunnerFactory;

  @PostConstruct
  public void init() {
    // first, check all internal runner
    logger.info("----- CherryMain.1 Load all embedded runner");
    runnerFactory.init();

    // at this point, the table is up-to-date, class loader is correct : let's start all runners
    logger.info("----- CherryMain.4 Start all runners");
    cherryJobRunnerFactory.startAll();
  }

  @PreDestroy
  public void end() {
    cherryJobRunnerFactory.stopAll();

  }

}
