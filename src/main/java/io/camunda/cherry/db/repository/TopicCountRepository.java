package io.camunda.cherry.db.repository;

import io.camunda.cherry.db.entity.TopicCountEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Transactional
public interface TopicCountRepository extends JpaRepository<TopicCountEntity, Long> {

    @Query("select tc from TopicCountEntity tc"
            + " where tc.executionTime >= :dateToSearch"
            + " and tc.runnerType = :runnerType"
            + " order by tc.executionTime desc")
    List<TopicCountEntity> selectRunnerRecords(@Param("runnerType") String runnerType,
                                               @Param("dateToSearch") LocalDateTime dateToSearch,
                                               Pageable pageable);

    @Modifying
    @Query("delete from TopicCountEntity tc where tc.runnerType = :runnerType")
    void deleteFromRunnerType(@Param("runnerType") String runnerType);
}
