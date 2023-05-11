package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.RunnerDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RunnerDefinitionRepository extends JpaRepository<RunnerDefinitionEntity, Long> {

  @Query(
      "select runnerDefinition from RunnerDefinitionEntity runnerDefinition" + " where runnerDefinition.name = :name ")
  RunnerDefinitionEntity selectByName(@Param("name") String name);

  @Query(
      "select runnerDefinition from RunnerDefinitionEntity runnerDefinition" + " where runnerDefinition.type = :type ")
  RunnerDefinitionEntity selectByType(@Param("type") String type);

  @Query(
      "select runnerDefinition from RunnerDefinitionEntity runnerDefinition" + " where runnerDefinition.type not in :listTypes ")
  List<RunnerDefinitionEntity> selectNotInType(@Param("listTypes") List<String> listTypes);

  @Query("select runnerDefinition from RunnerDefinitionEntity runnerDefinition"
      + " where runnerDefinition.jar is not null")
  List<RunnerDefinitionEntity> selectAllByJarNotNull();
}
