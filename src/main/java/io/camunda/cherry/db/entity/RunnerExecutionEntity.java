package io.camunda.cherry.db.entity;


import io.camunda.cherry.definition.AbstractRunner;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "RUNNEREXECUTION")
public class RunnerExecutionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(name = "runner_name", length = 100)
  public String  runnerName;

  @Column(name = "execution_time")
  public Instant executionTime;


  @Column(name = "execution_ms")
  public Long executionMs;

  @Column(name = "status", length = 100)
  @Enumerated(EnumType.STRING)
  public AbstractRunner.ExecutionStatusEnum status;

  // Save Log information: https://github.com/janzyka/blobs-jpa/

}
