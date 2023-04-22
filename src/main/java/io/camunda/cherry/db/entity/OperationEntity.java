package io.camunda.cherry.db.entity;

import io.camunda.cherry.definition.AbstractRunner;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "OPERATION")
public class OperationEntity {

  @Column(name = "hostname", length = 100)
  public String hostName;


  @Column(name = "runner_type", length = 100)
  public String runnerType;

  @Column(name = "execution_time")
  public Instant executionTime;


  @Column(name = "operationname", length = 50)
  @Enumerated(EnumType.STRING)
  public Operation operation;

  @Id
  @SequenceGenerator(name = "seqoperation", sequenceName = "seqoperation", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  public enum Operation {HOSTNAME, STARTRUNNER, STOPRUNNER, SETTHRESOLD, STOPRUNTIME, STARTRUNTIME }

}
