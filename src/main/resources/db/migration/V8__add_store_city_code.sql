-- 매장 도시 식별자를 추가하고 확인된 서울 매장 정보를 보완합니다.
ALTER TABLE stores ADD COLUMN city_code VARCHAR(100) NULL;

UPDATE stores
SET city_code = 'SEOUL'
WHERE code = 'MCM-SEOUL' AND country_code = 'KR' AND city_code IS NULL;
