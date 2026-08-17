-- 제품 Variant에서 사용하지 않는 이미지 객체 키를 제거하는 마이그레이션
ALTER TABLE product_variants
    DROP COLUMN image_object_key;
