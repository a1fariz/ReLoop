package com.reloop.ledger.repository;

import com.reloop.ledger.domain.FinancialLedgerLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FinancialLedgerLineRepository extends JpaRepository<FinancialLedgerLine, UUID> {
}
