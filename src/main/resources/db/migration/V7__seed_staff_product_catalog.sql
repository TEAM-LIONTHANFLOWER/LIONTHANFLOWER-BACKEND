-- 직원 Arc와 Visit Memory 생성에 사용할 기본 제품 카탈로그를 초기화하는 마이그레이션
INSERT INTO products (id, external_product_code, name, category, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000101',
    'MCM-BAG-001',
    'Stark Backpack',
    'BAG',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE external_product_code = 'MCM-BAG-001'
);

INSERT INTO products (id, external_product_code, name, category, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000102',
    'MCM-ACCESSORY-001',
    'Himmel Wristlet',
    'ACCESSORY',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE external_product_code = 'MCM-ACCESSORY-001'
);

INSERT INTO products (id, external_product_code, name, category, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000103',
    'MCM-CLOTHING-001',
    'Essentials Hoodie',
    'CLOTHING',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE external_product_code = 'MCM-CLOTHING-001'
);

INSERT INTO product_variants (
    id, product_id, external_variant_code, color, size_option, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000201',
    p.id,
    'MCM-BAG-001-BLACK-M',
    'BLACK',
    'M',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM products p
WHERE p.external_product_code = 'MCM-BAG-001'
  AND NOT EXISTS (
      SELECT 1 FROM product_variants WHERE external_variant_code = 'MCM-BAG-001-BLACK-M'
  );

INSERT INTO product_variants (
    id, product_id, external_variant_code, color, size_option, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000202',
    p.id,
    'MCM-BAG-001-WHITE-M',
    'WHITE',
    'M',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM products p
WHERE p.external_product_code = 'MCM-BAG-001'
  AND NOT EXISTS (
      SELECT 1 FROM product_variants WHERE external_variant_code = 'MCM-BAG-001-WHITE-M'
  );

INSERT INTO product_variants (
    id, product_id, external_variant_code, color, size_option, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000203',
    p.id,
    'MCM-ACCESSORY-001-BLACK-ONE',
    'BLACK',
    'ONE_SIZE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM products p
WHERE p.external_product_code = 'MCM-ACCESSORY-001'
  AND NOT EXISTS (
      SELECT 1
      FROM product_variants
      WHERE external_variant_code = 'MCM-ACCESSORY-001-BLACK-ONE'
  );

INSERT INTO product_variants (
    id, product_id, external_variant_code, color, size_option, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000204',
    p.id,
    'MCM-CLOTHING-001-BLACK-S',
    'BLACK',
    'S',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM products p
WHERE p.external_product_code = 'MCM-CLOTHING-001'
  AND NOT EXISTS (
      SELECT 1
      FROM product_variants
      WHERE external_variant_code = 'MCM-CLOTHING-001-BLACK-S'
  );

INSERT INTO product_variants (
    id, product_id, external_variant_code, color, size_option, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000205',
    p.id,
    'MCM-CLOTHING-001-BLACK-M',
    'BLACK',
    'M',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM products p
WHERE p.external_product_code = 'MCM-CLOTHING-001'
  AND NOT EXISTS (
      SELECT 1
      FROM product_variants
      WHERE external_variant_code = 'MCM-CLOTHING-001-BLACK-M'
  );
