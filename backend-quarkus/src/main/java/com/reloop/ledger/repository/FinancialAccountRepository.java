package com.reloop.ledger.repository;

import com.reloop.ledger.domain.FinancialAccount;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FinancialAccountRepository implements ReloopRepository<FinancialAccount, UUID> {

    public Optional<FinancialAccount> findByCode(String code) {
        return find("code", code).firstResultOptional();
    }
}
