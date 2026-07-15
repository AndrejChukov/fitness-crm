package ru.fitnesscrm.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.fitnesscrm.common.domain.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private boolean active = true;
}
