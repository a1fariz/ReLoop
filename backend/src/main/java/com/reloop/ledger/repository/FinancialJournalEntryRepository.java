package com.reloop.ledger.repository;

import com.reloop.ledger.domain.FinancialJournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FinancialJournalEntryRepository extends JpaRepository<FinancialJournalEntry, UUID> {
}
