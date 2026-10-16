-- 이메일 도메인의 소문자 저장을 강제하여 직접 SQL 입력의 정규화 우회를 방지합니다.
ALTER TABLE customer_email_identities
    ADD CONSTRAINT ck_email_identity_lowercase_domain
    CHECK (REGEXP_LIKE(email, '^[^@]+@[a-z0-9.-]+$', 'c'));

ALTER TABLE email_verification_requests
    ADD CONSTRAINT ck_email_request_lowercase_domain
    CHECK (REGEXP_LIKE(email, '^[^@]+@[a-z0-9.-]+$', 'c'));
