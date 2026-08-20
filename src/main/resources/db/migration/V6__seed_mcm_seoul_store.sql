-- 고객 방문 생성에 필요한 MCM 서울 기본 매장을 초기화하는 마이그레이션
INSERT INTO stores (id, name, code, country_code, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000001',
    'MCM Seoul',
    'MCM-SEOUL',
    'KR',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE code = 'MCM-SEOUL');
