package io.camunda.cherry.db.entity;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChJarstorage")
public class JarStorageEntity {

  @Column(name = "name", length = 1000, unique = true)
  public String name;

  @Lob
  @Column(name = "jarfile")
  // @ T y  p e(type = "org.hibernate.type.BinaryType") for Postgres


  public byte[] jarfile;

  @Column(name = "load_log", length = 2000)
  public String loadLog;

  /**
   * execution (in UTC) Instant will saved the date.. in the local timezone (example 15:04), that we
   * don't want (and make no sense) We want to save the date in UTC, so let's use a LocalDateTime,
   * and the code is reponsible to provide this time in the UTC time zone.
   */
  @Column(name = "loaded_time")
  public LocalDateTime loadedTime;

  @Id
  @SequenceGenerator(name = "jarconnectors", sequenceName = "jarconnectors", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  public Long id;
}
