package ru.fitnesscrm.finance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.fitnesscrm.finance.domain.ClientAccount;

import java.util.Optional;

public interface ClientAccountRepository extends JpaRepository<ClientAccount, Long> {

    Optional<ClientAccount> findByClientId(Long clientId);
}
