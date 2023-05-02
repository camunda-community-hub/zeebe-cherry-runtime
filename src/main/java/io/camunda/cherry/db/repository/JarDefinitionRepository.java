/* ******************************************************************** */
/*                                                                      */
/*  JarDefinitionRepository                                             */
/*                                                                      */
/*  Save Jar file                                                       */
/* ******************************************************************** */
package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.JarStorageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JarDefinitionRepository extends JpaRepository<JarStorageEntity, Long> {

  @Query("select jarDefinition from JarStorageEntity jarDefinition" + " where jarDefinition.name = :name ")
  JarStorageEntity findByName(@Param("name") String name);

  @Query("select jarDefinition from JarStorageEntity jarDefinition")
  List<JarStorageEntity> getAll();
}
