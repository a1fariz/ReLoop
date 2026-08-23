package com.reloop.ledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "financial_accounts")
public class FinancialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountType accountType;

    @Column(nullable = false, length = 3)
    private String currency = "IDR";

    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum AccountType {
        ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
    }

    public FinancialAccount() {}

    public FinancialAccount(String code, AccountType accountType, String currency, String description) {
        this.code = code;
        this.accountType = accountType;
        this.currency = currency != null ? currency : "IDR";
        this.description = description;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public AccountType getAccountType() { return accountType; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
