package io.camunda.cherry.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Blob;
import java.sql.Types;
import java.time.LocalDateTime;


/* ******************************************************************** */
/*                                                                      */
/*  Jar storage entity                                                  */
/*                                                                      */
/*  Save JAR.                                                           */
/*  One JAR can contain MULTIPLE connectors                             */
/*  Connector (runner) are saved in the RunnerDefinition Entity         */
/* ******************************************************************** */

@Entity
@Table(name = "ChJarstorage")
public class JarStorageEntity {

  /**
   * The Jar Name (file name)
   */
  @Column(name = "name", length = 1000, unique = true)
  public String name;

  /**
   * Database H2, save the JAR
   * in H2, the byte[] have an issue, and large file can't be saved by this way.
   */
  @Lob
  @Column(name = "jarfile")
  public Blob jarfileBlob;

  /**
   * Database Postgres, save the JAR
   * Blob is saved as a OID in Postgres if this is not explicitly a byte[] colum
   */
  @Lob
  @Column(name = "jarfilebyte")
  @JdbcTypeCode(Types.BINARY)
  public byte[] jarfileByte;

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
