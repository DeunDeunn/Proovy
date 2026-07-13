ALTER TABLE cash_transactions DROP CONSTRAINT cash_transactions_type_check;
ALTER TABLE cash_transactions ADD CONSTRAINT cash_transactions_type_check
    CHECK (type IN ('CHARGE', 'CHALLENGE_HOLD', 'CHALLENGE_PRINCIPAL_REFUND',
                    'CHALLENGE_PRINCIPAL_SUCCESS', 'CHALLENGE_PRINCIPAL_FAIL',
                    'CHALLENGE_PROFIT_DISTRIBUTION', 'HOST_FEE', 'WITHDRAWAL',
                    'WITHDRAWAL_REFUND', 'AI_TICKET_PURCHASE', 'AI_TICKET_REFUND'));
