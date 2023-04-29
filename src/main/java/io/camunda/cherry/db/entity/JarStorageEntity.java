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

@Entity
@Table(name = "JARSTORAGE")
public class JarStorageEntity {

  @Column(name = "name", length = 1000, unique = true)
  public String name;

  @Lob
  @Column(name = "jarfile")
  @Type(type = "org.hibernate.type.BinaryType")
  public byte[] jarfile;

  @Column(name = "loadlog", length = 2000)
  public String loadLog;


  @Id
  @SequenceGenerator(name = "jarconnectors", sequenceName = "jarconnectors", allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  public Long id;

}
