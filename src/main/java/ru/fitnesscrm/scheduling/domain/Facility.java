package ru.fitnesscrm.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.fitnesscrm.common.domain.TenantEntity;

@Getter
@Setter
@Entity
@Table(name = "facilities")
public class Facility extends TenantEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private boolean active = true;
}
