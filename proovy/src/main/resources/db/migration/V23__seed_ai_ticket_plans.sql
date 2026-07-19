INSERT INTO ai_ticket_plans (name, duration_days, price, description, active)
SELECT '1일 AI 티켓', 1, 1000, '24시간 동안 AI 검수 기능 사용 가능', true
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_ticket_plans
    WHERE name = '1일 AI 티켓'
      AND duration_days = 1
);

INSERT INTO ai_ticket_plans (name, duration_days, price, description, active)
SELECT '7일 AI 티켓', 7, 5000, '7일 동안 AI 검수 기능 사용 가능', true
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_ticket_plans
    WHERE name = '7일 AI 티켓'
      AND duration_days = 7
);

INSERT INTO ai_ticket_plans (name, duration_days, price, description, active)
SELECT '30일 AI 티켓', 30, 15000, '30일 동안 AI 검수 기능 사용 가능', true
WHERE NOT EXISTS (
    SELECT 1
    FROM ai_ticket_plans
    WHERE name = '30일 AI 티켓'
      AND duration_days = 30
);
