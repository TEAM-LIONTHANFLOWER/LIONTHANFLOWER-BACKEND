-- 초기 도메인 엔티티 테이블과 제약조건을 생성하는 마이그레이션
CREATE TABLE stores (
    id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_stores PRIMARY KEY (id),
    CONSTRAINT uk_stores_code UNIQUE (code)
);

CREATE TABLE customers (
    id CHAR(36) NOT NULL,
    name VARCHAR(100) NULL,
    token_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_token_hash UNIQUE (token_hash)
);

CREATE TABLE staff (
    id CHAR(36) NOT NULL,
    store_id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(2048) NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_staff PRIMARY KEY (id),
    CONSTRAINT fk_staff_store FOREIGN KEY (store_id) REFERENCES stores (id)
);

CREATE TABLE store_devices (
    id CHAR(36) NOT NULL,
    store_id CHAR(36) NOT NULL,
    selected_staff_id CHAR(36) NULL,
    name VARCHAR(100) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_store_devices PRIMARY KEY (id),
    CONSTRAINT uk_store_devices_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_store_devices_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_store_devices_staff FOREIGN KEY (selected_staff_id) REFERENCES staff (id)
);

CREATE TABLE visits (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    store_id CHAR(36) NOT NULL,
    staff_id CHAR(36) NULL,
    store_device_id CHAR(36) NULL,
    waiting_number VARCHAR(40) NOT NULL,
    service_language VARCHAR(40) NULL,
    interaction_style VARCHAR(40) NULL,
    additional_request VARCHAR(1000) NULL,
    status VARCHAR(40) NOT NULL,
    arc_creation_granted_at TIMESTAMP(6) NULL,
    matched_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    canceled_at TIMESTAMP(6) NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_visits PRIMARY KEY (id),
    CONSTRAINT uk_visits_store_waiting_number UNIQUE (store_id, waiting_number),
    CONSTRAINT fk_visits_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_visits_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_visits_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
    CONSTRAINT fk_visits_store_device FOREIGN KEY (store_device_id) REFERENCES store_devices (id)
);

CREATE TABLE arcs (
    id CHAR(36) NOT NULL,
    visit_id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    image_object_key VARCHAR(1024) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_by_staff_id CHAR(36) NOT NULL,
    last_modified_by_staff_id CHAR(36) NOT NULL,
    confirmed_at TIMESTAMP(6) NULL,
    finalized_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_arcs PRIMARY KEY (id),
    CONSTRAINT uk_arcs_visit_id UNIQUE (visit_id),
    CONSTRAINT fk_arcs_visit FOREIGN KEY (visit_id) REFERENCES visits (id),
    CONSTRAINT fk_arcs_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_arcs_created_by_staff FOREIGN KEY (created_by_staff_id) REFERENCES staff (id),
    CONSTRAINT fk_arcs_last_modified_by_staff FOREIGN KEY (last_modified_by_staff_id) REFERENCES staff (id)
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
