package io.camunda.cherry.db.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChOperation")
public class OperationEntity {

  @Column(name = "hostname", length = 100)
  public String hostName;

  @Column(name = "runner_type", length = 100)
  public String runnerType;

  /**
   * execution (in UTC) Instant will saved the date.. in the local timezone (example 15:04), that we
   * don't want (and make no sense) We want to save the date in UTC, so let's use a LocalDateTime,
   * and the code is reponsible to provide this time in the UTC time zone.
   */
  @Column(name = "execution_time")
  public LocalDateTime executionTime;

  @Column(name = "operationname", length = 50)
  @Enumerated(EnumType.STRING)
  public Operation operation;

  @Column(name = "message", length = 3000)
  public String message;

  @Id
  @SequenceGenerator(name = "seqoperation", sequenceName = "seqoperation", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  public enum Operation {
    HOSTNAME, STARTRUNNER, STOPRUNNER, SETTHRESOLD, STOPRUNTIME, STARTRUNTIME, SERVERINFO, ERROR
  }
}
