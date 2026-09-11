package com.reloop.ledger.repository;

import com.reloop.ledger.domain.FinancialJournalEntry;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FinancialJournalEntryRepository implements ReloopRepository<FinancialJournalEntry, UUID> {

    public Optional<FinancialJournalEntry> findByReferenceTypeAndReferenceId(String referenceType, String referenceId) {
        return find("referenceType = ?1 AND referenceId = ?2", referenceType, referenceId).firstResultOptional();
    }

    public List<FinancialJournalEntry> findByReferenceId(String referenceId) {
        return list("referenceId", referenceId);
    }
}
