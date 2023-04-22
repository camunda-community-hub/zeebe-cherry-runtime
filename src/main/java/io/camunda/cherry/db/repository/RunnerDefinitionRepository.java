package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RunnerDefinitionRepository extends JpaRepository<RunnerDefinitionEntity, Long> {

  @Query("select connectorDefinition from RunnerDefinitionEntity connectorDefinition"
      + " where connectorDefinition.name = :name ")
  RunnerDefinitionEntity selectByName(@Param("name") String name);

}
