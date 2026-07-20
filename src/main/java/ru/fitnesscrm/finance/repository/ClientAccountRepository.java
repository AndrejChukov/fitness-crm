package ru.fitnesscrm.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.fitnesscrm.finance.domain.ClientAccount;

import java.math.BigDecimal;
import java.util.Optional;

public interface ClientAccountRepository extends JpaRepository<ClientAccount, Long> {

    Optional<ClientAccount> findByClientId(Long clientId);

    @Modifying(clearAutomatically = true)
    @Query("""
        update ClientAccount ca set ca.balance = ca.balance + :debt
        where ca.clientId = :unpaidClientId
    """)
    int applyDebt(@Param("unpaidClientId") Long unpaidClientId, @Param("debt") BigDecimal debt);

}
