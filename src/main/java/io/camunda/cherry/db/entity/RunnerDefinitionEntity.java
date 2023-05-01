package io.camunda.cherry.db.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "ChRunnerdef")
public class RunnerDefinitionEntity {

  @Column(name = "name", length = 300)
  public String name;

  @Column(name = "classname", length = 1000, unique = true)
  public String classname;

  @Column(name = "collection", length = 300)
  public String collectionName;

  @Column(name = "type", length = 1000)
  public String type;

  @Column(name = "origin", length = 1000)
  @Enumerated(EnumType.STRING)
  public Origin origin;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "jarid")
  public JarStorageEntity jar;

  @Column(name = "githubrepo", length = 1000)
  public String githubRepo;

  @Column(name = "release", length = 50)
  public String release;

  @Column(name = "activerunner")
  public boolean activeRunner;
  @Id
  @SequenceGenerator(name = "seqconnectors", sequenceName = "seqconnectors", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  public Long id;

  public enum Origin {EMBEDDED, JARFILE, STORE}

}
