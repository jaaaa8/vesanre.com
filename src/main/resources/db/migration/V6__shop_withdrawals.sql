-- Shop coin withdrawals: coins leave the wallet at request time; a rejection returns them.
CREATE TABLE sporthub.shop_withdrawals (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES sporthub.shops (id) ON DELETE RESTRICT,
    requested_by uuid NOT NULL REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    coin_amount bigint NOT NULL,
    fee_coins bigint NOT NULL,
    payout_vnd numeric(14,2) NOT NULL,
    bank_name varchar(120) NOT NULL,
    bank_account_number varchar(40) NOT NULL,
    bank_account_name varchar(160) NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    bank_transaction_ref varchar(120),
    rejection_reason text,
    reviewed_by uuid REFERENCES sporthub.users (id) ON DELETE RESTRICT,
    reviewed_at timestamptz,
    paid_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_shop_withdrawals_amount CHECK (
        coin_amount >= 100 AND fee_coins > 0 AND fee_coins < coin_amount
        AND payout_vnd = (coin_amount - fee_coins) * 1000),
    CONSTRAINT ck_shop_withdrawals_status CHECK (status IN ('PENDING', 'PAID', 'REJECTED')),
    CONSTRAINT ck_shop_withdrawals_review CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL AND paid_at IS NULL)
        OR (status = 'PAID' AND reviewed_by IS NOT NULL AND paid_at IS NOT NULL AND bank_transaction_ref IS NOT NULL)
        OR (status = 'REJECTED' AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL AND rejection_reason IS NOT NULL))
);
CREATE INDEX ix_shop_withdrawals_shop_created ON sporthub.shop_withdrawals (shop_id, created_at DESC);
CREATE INDEX ix_shop_withdrawals_status_created ON sporthub.shop_withdrawals (status, created_at);

ALTER TABLE sporthub.coin_ledger_entries
    ADD COLUMN withdrawal_id uuid REFERENCES sporthub.shop_withdrawals (id) ON DELETE RESTRICT;

ALTER TABLE sporthub.coin_ledger_entries DROP CONSTRAINT ck_coin_ledger_entries_type;
ALTER TABLE sporthub.coin_ledger_entries ADD CONSTRAINT ck_coin_ledger_entries_type CHECK (
    (entry_type = 'TOP_UP' AND amount > 0 AND top_up_id IS NOT NULL AND booking_id IS NULL AND withdrawal_id IS NULL)
    OR (entry_type = 'BOOKING_DEPOSIT' AND amount < 0 AND booking_id IS NOT NULL AND top_up_id IS NULL AND withdrawal_id IS NULL)
    OR (entry_type IN ('BOOKING_DEPOSIT_RETURN', 'BOOKING_PAYOUT') AND amount > 0 AND booking_id IS NOT NULL AND top_up_id IS NULL AND withdrawal_id IS NULL)
    OR (entry_type = 'WITHDRAWAL' AND amount < 0 AND withdrawal_id IS NOT NULL AND booking_id IS NULL AND top_up_id IS NULL)
    OR (entry_type = 'WITHDRAWAL_RETURN' AND amount > 0 AND withdrawal_id IS NOT NULL AND booking_id IS NULL AND top_up_id IS NULL)
);

-- One debit and at most one return per withdrawal.
CREATE UNIQUE INDEX uq_coin_ledger_entries_withdrawal_type
    ON sporthub.coin_ledger_entries (withdrawal_id, entry_type) WHERE withdrawal_id IS NOT NULL;
