package com.reloop.ledger.repository;

import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class FinancialLedgerLineRepository implements ReloopRepository<FinancialLedgerLine, UUID> {
}
