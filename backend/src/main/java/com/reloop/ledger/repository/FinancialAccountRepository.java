package com.reloop.ledger.repository;

import com.reloop.ledger.domain.FinancialAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialAccountRepository extends JpaRepository<FinancialAccount, UUID> {
    Optional<FinancialAccount> findByCode(String code);
}
