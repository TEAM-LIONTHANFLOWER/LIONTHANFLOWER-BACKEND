-- 고객 방문 매장 선택에 사용할 파리와 뮌헨 매장을 초기화합니다.
INSERT INTO stores (id, name, code, country_code, city_code, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000002',
    'MCM Paris',
    'MCM-PARIS',
    'FR',
    'PARIS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE code = 'MCM-PARIS');

INSERT INTO stores (id, name, code, country_code, city_code, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000003',
    'MCM Munich',
    'MCM-MUNICH',
    'DE',
    'MUNICH',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE code = 'MCM-MUNICH');
