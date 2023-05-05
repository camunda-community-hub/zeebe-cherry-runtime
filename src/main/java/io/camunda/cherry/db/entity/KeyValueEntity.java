package io.camunda.cherry.db.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "ChKeyvalue", indexes = { @Index(name = "unique_index", columnList = "origin,name", unique = true) })
public class KeyValueEntity {

  @Column(name = "name", length = 1000)
  public String name;

  @Column(name = "valuekey", length = 1000)
  public String valueKey;

  @Column(name = "origin", length = 10)
  @Enumerated(EnumType.STRING)
  public KeyValueType origin;

  @Column(name = "issecret")
  public boolean isSecret;

  @Id
  @SequenceGenerator(name = "seqkeyvalue", sequenceName = "seqkeyvalue", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  public Long id;

  public enum KeyValueType {
    SECRET, ENV
  }
}
