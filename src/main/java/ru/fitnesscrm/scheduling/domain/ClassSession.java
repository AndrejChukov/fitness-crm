package ru.fitnesscrm.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import ru.fitnesscrm.common.domain.TenantEntity;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "class_sessions")
public class ClassSession extends TenantEntity {

    @Column(name = "facility_id", nullable = false)
    private Long facilityId;

    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Version
    private Long version;
}
