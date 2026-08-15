-- IA 기준 엔티티 구조로 기존 개발 스키마를 재구성하는 마이그레이션
DROP TABLE IF EXISTS myself_images;
DROP TABLE IF EXISTS arcs;
DROP TABLE IF EXISTS visits;
DROP TABLE IF EXISTS store_devices;
DROP TABLE IF EXISTS staff;

ALTER TABLE stores
    ADD COLUMN country_code VARCHAR(2) NOT NULL DEFAULT 'KR';

CREATE TABLE staff (
    id CHAR(36) NOT NULL,
    store_id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_staff PRIMARY KEY (id),
    CONSTRAINT uk_staff_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_staff_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE staff_languages (
    staff_id CHAR(36) NOT NULL,
    language VARCHAR(40) NOT NULL,
    CONSTRAINT pk_staff_languages PRIMARY KEY (staff_id, language),
    CONSTRAINT uk_staff_languages_staff_language UNIQUE (staff_id, language),
    CONSTRAINT fk_staff_languages_staff FOREIGN KEY (staff_id) REFERENCES staff (id)
);

CREATE TABLE visits (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    store_id CHAR(36) NOT NULL,
    staff_id CHAR(36) NULL,
    service_language VARCHAR(40) NULL,
    interaction_style VARCHAR(40) NULL,
    additional_request VARCHAR(1000) NULL,
    status VARCHAR(40) NOT NULL,
    purchase_decision VARCHAR(40) NOT NULL,
    purchase_decided_by_staff_id CHAR(36) NULL,
    purchase_decided_at TIMESTAMP(6) NULL,
    matched_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    canceled_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_visits PRIMARY KEY (id),
    CONSTRAINT fk_visits_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_visits_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_visits_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
    CONSTRAINT fk_visits_purchase_staff
        FOREIGN KEY (purchase_decided_by_staff_id) REFERENCES staff (id)
);

CREATE TABLE products (
    id CHAR(36) NOT NULL,
    external_product_code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_external_product_code UNIQUE (external_product_code)
);

CREATE TABLE product_variants (
    id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    external_variant_code VARCHAR(100) NOT NULL,
    image_object_key VARCHAR(1024) NOT NULL,
    color VARCHAR(40) NOT NULL,
    option VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_product_variants PRIMARY KEY (id),
    CONSTRAINT uk_product_variants_external_variant_code UNIQUE (external_variant_code),
    CONSTRAINT fk_product_variants_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE purchases (
    id CHAR(36) NOT NULL,
    visit_id CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_purchases PRIMARY KEY (id),
    CONSTRAINT uk_purchases_visit_id UNIQUE (visit_id),
    CONSTRAINT fk_purchases_visit FOREIGN KEY (visit_id) REFERENCES visits (id)
);

CREATE TABLE purchase_items (
    id CHAR(36) NOT NULL,
    purchase_id CHAR(36) NOT NULL,
    product_variant_id CHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_purchase_items PRIMARY KEY (id),
    CONSTRAINT uk_purchase_items_purchase_variant UNIQUE (purchase_id, product_variant_id),
    CONSTRAINT fk_purchase_items_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    CONSTRAINT fk_purchase_items_variant
        FOREIGN KEY (product_variant_id) REFERENCES product_variants (id)
);

CREATE TABLE arcs (
    id CHAR(36) NOT NULL,
    visit_id CHAR(36) NOT NULL,
    purchase_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    created_by_staff_id CHAR(36) NOT NULL,
    shared_revision_id CHAR(36) NULL,
    final_revision_id CHAR(36) NULL,
    status VARCHAR(40) NOT NULL,
    shared_at TIMESTAMP(6) NULL,
    finalized_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_arcs PRIMARY KEY (id),
    CONSTRAINT uk_arcs_visit_id UNIQUE (visit_id),
    CONSTRAINT uk_arcs_purchase_id UNIQUE (purchase_id),
    CONSTRAINT fk_arcs_visit FOREIGN KEY (visit_id) REFERENCES visits (id),
    CONSTRAINT fk_arcs_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    CONSTRAINT fk_arcs_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_arcs_created_by_staff FOREIGN KEY (created_by_staff_id) REFERENCES staff (id)
);

CREATE TABLE arc_revisions (
    id CHAR(36) NOT NULL,
    arc_id CHAR(36) NOT NULL,
    revision_number INT NOT NULL,
    input_snapshot TEXT NOT NULL,
    generated_content TEXT NULL,
    template_version VARCHAR(100) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_by_staff_id CHAR(36) NOT NULL,
    failure_code VARCHAR(100) NULL,
    generated_at TIMESTAMP(6) NULL,
    shared_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_arc_revisions PRIMARY KEY (id),
    CONSTRAINT uk_arc_revisions_arc_number UNIQUE (arc_id, revision_number),
    CONSTRAINT fk_arc_revisions_arc FOREIGN KEY (arc_id) REFERENCES arcs (id),
    CONSTRAINT fk_arc_revisions_staff FOREIGN KEY (created_by_staff_id) REFERENCES staff (id)
);

ALTER TABLE arcs
    ADD CONSTRAINT fk_arcs_shared_revision
        FOREIGN KEY (shared_revision_id) REFERENCES arc_revisions (id),
    ADD CONSTRAINT fk_arcs_final_revision
        FOREIGN KEY (final_revision_id) REFERENCES arc_revisions (id);

CREATE TABLE visit_memories (
    id CHAR(36) NOT NULL,
    visit_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    created_by_staff_id CHAR(36) NOT NULL,
    input_snapshot TEXT NOT NULL,
    generated_content TEXT NULL,
    template_version VARCHAR(100) NOT NULL,
    status VARCHAR(40) NOT NULL,
    failure_code VARCHAR(100) NULL,
    generated_at TIMESTAMP(6) NULL,
    finalized_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_visit_memories PRIMARY KEY (id),
    CONSTRAINT uk_visit_memories_visit_id UNIQUE (visit_id),
    CONSTRAINT fk_visit_memories_visit FOREIGN KEY (visit_id) REFERENCES visits (id),
    CONSTRAINT fk_visit_memories_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_visit_memories_staff FOREIGN KEY (created_by_staff_id) REFERENCES staff (id)
);

CREATE TABLE myself_images (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    visit_id CHAR(36) NOT NULL,
    frame_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    source_image_object_key VARCHAR(1024) NULL,
    result_image_object_key VARCHAR(1024) NULL,
    failure_code VARCHAR(100) NULL,
    completed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_myself_images PRIMARY KEY (id),
    CONSTRAINT fk_myself_images_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_myself_images_visit FOREIGN KEY (visit_id) REFERENCES visits (id)
);

CREATE INDEX idx_visits_store_status_created_at
    ON visits (store_id, status, created_at);

CREATE INDEX idx_visits_staff_status
    ON visits (staff_id, status);

CREATE INDEX idx_arcs_customer_created_at
    ON arcs (customer_id, created_at);
