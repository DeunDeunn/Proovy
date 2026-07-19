CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE ai_ticket_subscriptions
    ADD CONSTRAINT ex_ai_ticket_subscriptions_active_period
    EXCLUDE USING gist (
        host_id WITH =,
        tsrange(started_at, expired_at, '[)') WITH &&
    )
    WHERE (status = 'ACTIVE');
