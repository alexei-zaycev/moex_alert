INSERT INTO tickers (id, created_at, updated_at, name, threshold, currency)
VALUES 
    (gen_random_uuid(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SBER', 0.1, 'RUB'),
    (gen_random_uuid(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'YDEX', 0.1, 'RUB'),
    (gen_random_uuid(), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'X5', 0.1, 'RUB')
ON CONFLICT (name) DO NOTHING;
