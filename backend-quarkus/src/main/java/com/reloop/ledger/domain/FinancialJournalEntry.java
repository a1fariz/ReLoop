package com.reloop.ledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "financial_journal_entries")
public class FinancialJournalEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String referenceType;

    @Column(nullable = false, length = 100)
    private String referenceId;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public FinancialJournalEntry() {}

    public FinancialJournalEntry(String referenceType, String referenceId, String description) {
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
    }

    public UUID getId() { return id; }
    public String getReferenceType() { return referenceType; }
    public String getReferenceId() { return referenceId; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
