package ru.fitnesscrm.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ru.fitnesscrm.common.domain.TenantEntity;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "client_accounts")
public class ClientAccount extends TenantEntity {

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
}
