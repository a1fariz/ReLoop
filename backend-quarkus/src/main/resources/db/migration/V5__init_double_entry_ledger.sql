-- V5: Double-Entry Financial Accounting Ledger

CREATE TABLE financial_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE, -- e.g. 'GATEWAY_CLEARING', 'ESCROW_HELD', 'PLATFORM_REVENUE', 'SELLER_PAYABLE:101'
    account_type VARCHAR(30) NOT NULL, -- 'ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'
    currency VARCHAR(3) NOT NULL DEFAULT 'IDR',
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE financial_journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_type VARCHAR(50) NOT NULL, -- 'ORDER_PAYMENT', 'ESCROW_SETTLEMENT', 'PARTIAL_REFUND', 'PAYOUT_WITHDRAWAL'
    reference_id VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_journal_reference ON financial_journal_entries(reference_type, reference_id);

CREATE TABLE financial_ledger_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_entry_id UUID NOT NULL REFERENCES financial_journal_entries(id) ON DELETE RESTRICT,
    account_id UUID NOT NULL REFERENCES financial_accounts(id) ON DELETE RESTRICT,
    entry_type VARCHAR(2) NOT NULL, -- 'DR' (Debit) or 'CR' (Credit)
    amount NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_valid_entry_type CHECK (entry_type IN ('DR', 'CR'))
);

CREATE INDEX idx_ledger_lines_account ON financial_ledger_lines(account_id);
CREATE INDEX idx_ledger_lines_journal ON financial_ledger_lines(journal_entry_id);

-- Initialize Standard System Financial Accounts
INSERT INTO financial_accounts (id, code, account_type, currency, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'GATEWAY_CLEARING', 'ASSET', 'IDR', 'Funds in transit from payment gateway'),
    ('00000000-0000-0000-0000-000000000002', 'ESCROW_HELD', 'LIABILITY', 'IDR', 'Buyer funds locked in platform escrow'),
    ('00000000-0000-0000-0000-000000000003', 'PLATFORM_REVENUE', 'REVENUE', 'IDR', 'Platform commission and take-rate revenue'),
    ('00000000-0000-0000-0000-000000000004', 'DISBURSEMENT_BANK', 'ASSET', 'IDR', 'Platform bank payout disbursement account');
