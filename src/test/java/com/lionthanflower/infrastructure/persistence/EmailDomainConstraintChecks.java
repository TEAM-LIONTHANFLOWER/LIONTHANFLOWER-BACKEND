// 직접 SQL로 이메일 도메인 제약조건과 중복 방지를 검증하는 공통 테스트 도우미
package com.lionthanflower.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.UUID;
import javax.sql.DataSource;

final class EmailDomainConstraintChecks {
  private EmailDomainConstraintChecks() {}

  static void verify(DataSource dataSource) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try {
        String customerId = UUID.randomUUID().toString();
        String identityId = UUID.randomUUID().toString();
        String requestId = UUID.randomUUID().toString();
        String local = "User" + UUID.randomUUID();
        String identityInsert =
            "INSERT INTO customer_email_identities (id, email, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        execute(
            connection,
            "INSERT INTO customers (id, token_hash, created_at, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            customerId,
            UUID.randomUUID().toString());
        reject(
            connection,
            "ck_email_identity_lowercase_domain",
            identityInsert,
            identityId,
            local + "@Example.COM");
        execute(connection, identityInsert, identityId, local + "@example.com");
        reject(
            connection,
            "uk_customer_email_identities_email",
            identityInsert,
            UUID.randomUUID().toString(),
            local + "@example.com");
        reject(
            connection,
            "ck_email_identity_lowercase_domain",
            "UPDATE customer_email_identities SET email = ? WHERE id = ?",
            local + "@EXAMPLE.com",
            identityId);
        String requestInsert =
            "INSERT INTO email_verification_requests (id, customer_id, email, purpose, code_hash, expires_at, created_at, updated_at) VALUES (?, ?, ?, 'ARCHIVE', 'hash', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        reject(
            connection,
            "ck_email_request_lowercase_domain",
            requestInsert,
            requestId,
            customerId,
            local + "@Example.COM");
        execute(connection, requestInsert, requestId, customerId, local + "@example.com");
        reject(
            connection,
            "ck_email_request_lowercase_domain",
            "UPDATE email_verification_requests SET email = ? WHERE id = ?",
            local + "@example.COM",
            requestId);
      } finally {
        connection.rollback();
      }
    }
  }

  private static void reject(Connection connection, String constraint, String sql, String... values)
      throws SQLException {
    Savepoint savepoint = connection.setSavepoint();
    try {
      assertThatThrownBy(() -> execute(connection, sql, values))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining(constraint);
    } finally {
      connection.rollback(savepoint);
      connection.releaseSavepoint(savepoint);
    }
  }

  private static void execute(Connection connection, String sql, String... values)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < values.length; i++) statement.setString(i + 1, values[i]);
      statement.executeUpdate();
    }
  }
}
