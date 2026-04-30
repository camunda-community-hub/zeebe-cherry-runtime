package io.camunda.cherry.db.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ChTopiccount")
public class TopicCountEntity {

    @Id
    @SequenceGenerator(name = "seqtopiccount", sequenceName = "seqtopiccount", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "runner_type", length = 1000)
    public String runnerType;

    @Column(name = "execution_time")
    public LocalDateTime executionTime;

    @Column(name = "topic_count")
    public long topicCount;
}
