package com.reloop.ledger;

import com.reloop.common.exception.BusinessException;
import com.reloop.ledger.domain.FinancialAccount;
import com.reloop.ledger.domain.FinancialJournalEntry;
import com.reloop.ledger.domain.FinancialLedgerLine;
import com.reloop.ledger.repository.FinancialAccountRepository;
import com.reloop.ledger.repository.FinancialJournalEntryRepository;
import com.reloop.ledger.repository.FinancialLedgerLineRepository;
import com.reloop.ledger.service.DoubleEntryLedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoubleEntryLedgerServiceTest {

    @Mock
    private FinancialAccountRepository accountRepository;
    @Mock
    private FinancialJournalEntryRepository journalRepository;
    @Mock
    private FinancialLedgerLineRepository ledgerLineRepository;

    private DoubleEntryLedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new DoubleEntryLedgerService(accountRepository, journalRepository, ledgerLineRepository);
    }

    @Test
    @DisplayName("Reject unbalanced journal entry where sum(Debits) != sum(Credits)")
    void testRejectUnbalancedJournal() {
        List<DoubleEntryLedgerService.PostingLine> lines = List.of(
                new DoubleEntryLedgerService.PostingLine("GATEWAY_CLEARING", FinancialLedgerLine.EntryType.DR, new BigDecimal("1000000.00")),
                new DoubleEntryLedgerService.PostingLine("ESCROW_HELD", FinancialLedgerLine.EntryType.CR, new BigDecimal("900000.00")) // Unbalanced!
        );

        assertThatThrownBy(() -> ledgerService.postJournal("ORDER_PAYMENT", "ORD-123", "Order payment", lines))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Double-entry unbalanced");
    }

    @Test
    @DisplayName("Successfully balance multi-line escrow settlement with platform fee deduction")
    void testSuccessfulMultiLineEscrowSettlement() {
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findByCode(any())).thenReturn(Optional.of(new FinancialAccount("CODE", FinancialAccount.AccountType.ASSET, "IDR", "desc")));
        when(journalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<DoubleEntryLedgerService.PostingLine> lines = List.of(
                new DoubleEntryLedgerService.PostingLine("ESCROW_HELD", FinancialLedgerLine.EntryType.DR, new BigDecimal("1000000.00")), // Total 1M debit
                new DoubleEntryLedgerService.PostingLine("SELLER_PAYABLE:101", FinancialLedgerLine.EntryType.CR, new BigDecimal("900000.00")), // 900k seller net
                new DoubleEntryLedgerService.PostingLine("PLATFORM_REVENUE", FinancialLedgerLine.EntryType.CR, new BigDecimal("100000.00")) // 100k platform fee
        );

        FinancialJournalEntry result = ledgerService.postJournal("ESCROW_SETTLEMENT", "ORD-123", "Release escrow", lines);
        assertThat(result).isNotNull();
    }
}
