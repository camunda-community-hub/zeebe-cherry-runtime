package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.KeyValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KeyValueRepository extends JpaRepository<KeyValueEntity, Long> {

  @Query("select keyValue from KeyValueEntity keyValue" + " where keyValue.name = :name "
      + " and keyValue.origin = :origin ")
  KeyValueEntity findByName(@Param("name") String name, @Param("origin") KeyValueEntity.KeyValueType origin);

  @Query("select keyValue from KeyValueEntity keyValue" + " where keyValue.origin = :origin ")
  List<KeyValueEntity> getAllByOrigin(@Param("origin") KeyValueEntity.KeyValueType origin);


}
