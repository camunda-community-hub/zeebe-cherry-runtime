package io.camunda.cherry.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/* ******************************************************************** */
/*                                                                      */
/*  RunnerDefinition entity                                             */
/*                                                                      */
/*  The runner is the connector to run. A connector OR a Cherry runner  */
/* ******************************************************************** */

@Entity
@Table(name = "ChRunnerdef")
public class RunnerDefinitionEntity {

  @Column(name = "name", length = 300)
  public String name;

  @Column(name = "classname", length = 1000)
  public String classname;

  @Column(name = "collection", length = 300)
  public String collectionName;

  @Column(name = "type", length = 1000, unique = true)
  public String type;

  @Column(name = "origin", length = 1000)
  @Enumerated(EnumType.STRING)
  public Origin origin;

  /**
   * If the Runner come from a JAR loaded (Upload, GitHub, directory) then the source is referenced here
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "jarid")
  public JarStorageEntity jar;

  @Column(name = "githubrepo", length = 1000)
  public String githubRepo;

  @Column(name = "release", length = 50)
  public String release;

  @Column(name = "activerunner")
  public boolean activeRunner;

  @Column(name = "status", length = 50)
  @Enumerated(EnumType.STRING)
  public Status status;

  @Id
  @SequenceGenerator(name = "seqconnectors", sequenceName = "seqconnectors", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  public Long id;

  public String toString() {
    return name + "(" + type + ")-" + origin;
  }

  public enum Origin {
    EMBEDDED, JARFILE, STORE
  }

  public enum Status {
    PRESENT, DELETED
  }
}
