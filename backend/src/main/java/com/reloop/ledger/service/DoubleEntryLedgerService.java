package com.reloop.ledger.service;

import com.reloop.common.exception.BusinessException;
import com.reloop.ledger.domain.FinancialAccount;
import com.reloop.ledger.domain.FinancialJournalEntry;
import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.ledger.repository.FinancialAccountRepository;
import com.reloop.ledger.repository.FinancialJournalEntryRepository;
import com.reloop.ledger.repository.FinancialLedgerLineRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class DoubleEntryLedgerService {
    private final FinancialAccountRepository accountRepository;
    private final FinancialJournalEntryRepository journalRepository;
    private final FinancialLedgerLineRepository ledgerLineRepository;

    public DoubleEntryLedgerService(
            FinancialAccountRepository accountRepository,
            FinancialJournalEntryRepository journalRepository,
            FinancialLedgerLineRepository ledgerLineRepository
    ) {
        this.accountRepository = accountRepository;
        this.journalRepository = journalRepository;
        this.ledgerLineRepository = ledgerLineRepository;
    }

    public record PostingLine(String accountCode, FinancialLedgerLine.EntryType entryType, BigDecimal amount) {}

    @Transactional
    public FinancialJournalEntry postJournal(String referenceType, String referenceId, String description, List<PostingLine> lines) {
        if (lines == null || lines.size() < 2) {
            throw new BusinessException("Journal entry requires at least 2 balancing lines", "INVALID_JOURNAL", HttpStatus.BAD_REQUEST);
        }

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (PostingLine line : lines) {
            if (line.amount() == null || line.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Ledger line amount must be strictly positive", "INVALID_AMOUNT", HttpStatus.BAD_REQUEST);
            }
            if (line.entryType() == FinancialLedgerLine.EntryType.DR) {
                totalDebit = totalDebit.add(line.amount());
            } else {
                totalCredit = totalCredit.add(line.amount());
            }
        }

        // Strict Balance Invariant Check: Sum(Debits) == Sum(Credits)
        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new BusinessException(
                    String.format("Double-entry unbalanced: Total Debit (%s) != Total Credit (%s)", totalDebit, totalCredit),
                    "UNBALANCED_JOURNAL",
                    HttpStatus.UNPROCESSABLE_ENTITY
            );
        }

        FinancialJournalEntry journal = journalRepository.save(new FinancialJournalEntry(referenceType, referenceId, description));

        for (PostingLine line : lines) {
            FinancialAccount account = accountRepository.findByCode(line.accountCode())
                    .orElseGet(() -> createSellerPayableAccountIfApplicable(line.accountCode()));

            FinancialLedgerLine ledgerLine = new FinancialLedgerLine(
                    journal.getId(),
                    account.getId(),
                    line.entryType(),
                    line.amount()
            );
            ledgerLineRepository.save(ledgerLine);
        }

        return journal;
    }

    private FinancialAccount createSellerPayableAccountIfApplicable(String accountCode) {
        if (accountCode.startsWith("SELLER_PAYABLE:")) {
            FinancialAccount newAccount = new FinancialAccount(
                    accountCode,
                    FinancialAccount.AccountType.LIABILITY,
                    "IDR",
                    "Wallet balance payable to seller " + accountCode.substring("SELLER_PAYABLE:".length())
            );
            return accountRepository.save(newAccount);
        }
        throw new BusinessException("Financial account not found: " + accountCode, "ACCOUNT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
