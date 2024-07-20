package io.camunda.cherry.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

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
