CREATE UNIQUE INDEX uq_ai_ticket_subscriptions_active_host
    ON ai_ticket_subscriptions(host_id)
    WHERE status = 'ACTIVE';
