-- 이메일 계정과 인증 요청 및 고객 인증 상태와 기록 보관 연결을 추가합니다.
CREATE TABLE customer_email_identities (
    id CHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_customer_email_identities PRIMARY KEY (id),
    CONSTRAINT uk_customer_email_identities_email UNIQUE (email)
);

CREATE TABLE email_verification_requests (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    consumed_at TIMESTAMP(6) NULL,
    invalidated_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_email_verification_requests PRIMARY KEY (id),
    CONSTRAINT fk_email_verification_requests_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_email_verification_attempts CHECK (failed_attempts >= 0)
);
CREATE INDEX idx_email_verification_email_created ON email_verification_requests (email, created_at);
CREATE INDEX idx_email_verification_customer_created ON email_verification_requests (customer_id, created_at);

ALTER TABLE customers
    ADD COLUMN email_identity_id CHAR(36) NULL,
    ADD COLUMN email_verified_at TIMESTAMP(6) NULL,
    ADD COLUMN email_auth_expires_at TIMESTAMP(6) NULL,
    ADD CONSTRAINT fk_customers_email_identity FOREIGN KEY (email_identity_id) REFERENCES customer_email_identities (id),
    ADD CONSTRAINT ck_customers_email_auth CHECK (
        (email_identity_id IS NULL AND email_verified_at IS NULL AND email_auth_expires_at IS NULL)
        OR (email_identity_id IS NOT NULL AND email_verified_at IS NOT NULL AND email_auth_expires_at IS NOT NULL
            AND email_auth_expires_at > email_verified_at)
    );
CREATE INDEX idx_customers_email_identity ON customers (email_identity_id);

ALTER TABLE arcs
    ADD COLUMN email_identity_id CHAR(36) NULL,
    ADD COLUMN archived_at TIMESTAMP(6) NULL,
    ADD CONSTRAINT fk_arcs_email_identity FOREIGN KEY (email_identity_id) REFERENCES customer_email_identities (id),
    ADD CONSTRAINT ck_arcs_email_archive CHECK (
        (email_identity_id IS NULL AND archived_at IS NULL)
        OR (email_identity_id IS NOT NULL AND archived_at IS NOT NULL)
    );
CREATE INDEX idx_arcs_email_archived ON arcs (email_identity_id, archived_at);

ALTER TABLE visit_memories
    ADD COLUMN email_identity_id CHAR(36) NULL,
    ADD COLUMN archived_at TIMESTAMP(6) NULL,
    ADD CONSTRAINT fk_visit_memories_email_identity FOREIGN KEY (email_identity_id) REFERENCES customer_email_identities (id),
    ADD CONSTRAINT ck_visit_memories_email_archive CHECK (
        (email_identity_id IS NULL AND archived_at IS NULL)
        OR (email_identity_id IS NOT NULL AND archived_at IS NOT NULL)
    );
CREATE INDEX idx_visit_memories_email_archived ON visit_memories (email_identity_id, archived_at);
