package com.reloop.ledger.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "financial_ledger_lines")
public class FinancialLedgerLine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID journalEntryId;

    @Column(nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private EntryType entryType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum EntryType {
        DR, CR
    }

    public FinancialLedgerLine() {}

    public FinancialLedgerLine(UUID journalEntryId, UUID accountId, EntryType entryType, BigDecimal amount) {
        this.journalEntryId = journalEntryId;
        this.accountId = accountId;
        this.entryType = entryType;
        this.amount = amount;
    }

    public UUID getId() { return id; }
    public UUID getJournalEntryId() { return journalEntryId; }
    public UUID getAccountId() { return accountId; }
    public EntryType getEntryType() { return entryType; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
