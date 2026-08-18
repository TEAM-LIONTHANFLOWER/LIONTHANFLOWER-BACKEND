-- 고객 웹 알림과 읽음 상태를 저장하는 테이블
CREATE TABLE customer_notifications (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    type VARCHAR(40) NOT NULL,
    resource_id CHAR(36) NOT NULL,
    message VARCHAR(255) NOT NULL,
    read_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_customer_notifications PRIMARY KEY (id),
    CONSTRAINT uk_customer_notifications_type_resource
        UNIQUE (customer_id, type, resource_id),
    CONSTRAINT fk_customer_notifications_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_customer_notifications_customer_created_at
    ON customer_notifications (customer_id, created_at);
