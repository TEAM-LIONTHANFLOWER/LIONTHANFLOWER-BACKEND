// 초기 도메인 스키마와 JPA 매핑의 일치 여부를 검증하는 통합 테스트
package com.lionthanflower.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionthanflower.support.PostgreSqlContainerSupport;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InitialDomainPersistenceTest extends PostgreSqlContainerSupport {

  @Autowired DataSource dataSource;

  @Test
  void 초기_도메인_테이블이_생성된다() throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                "select count(*) from information_schema.tables "
                    + "where table_schema = 'public' and table_name in "
                    + "('stores','staff','store_devices','customers','visits','arcs','myself_images')");
        ResultSet resultSet = statement.executeQuery()) {
      resultSet.next();
      assertThat(resultSet.getInt(1)).isEqualTo(7);
    }
  }

  @Test
  void 방문당_Arc는_하나만_저장할_수_있다() throws SQLException {
    PersistenceFixture fixture = PersistenceFixture.insertRequiredRows(dataSource);

    fixture.insertArc("00000000-0000-0000-0000-000000000101");

    assertThatThrownBy(() -> fixture.insertArc("00000000-0000-0000-0000-000000000102"))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void 존재하지_않는_방문으로_Arc를_저장할_수_없다() throws SQLException {
    PersistenceFixture fixture = PersistenceFixture.insertRequiredRows(dataSource);

    assertThatThrownBy(fixture::insertArcForUnknownVisit).isInstanceOf(SQLException.class);
  }
}

final class PersistenceFixture {

  private static final Timestamp TIMESTAMP = Timestamp.valueOf("2026-08-13 18:00:00");

  private final DataSource dataSource;
  private final String storeId = UUID.randomUUID().toString();
  private final String customerId = UUID.randomUUID().toString();
  private final String staffId = UUID.randomUUID().toString();
  private final String deviceId = UUID.randomUUID().toString();
  private final String visitId = UUID.randomUUID().toString();
  private final String waitingNumber = "A-" + UUID.randomUUID().toString().substring(0, 8);

  private PersistenceFixture(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  static PersistenceFixture insertRequiredRows(DataSource dataSource) throws SQLException {
    PersistenceFixture fixture = new PersistenceFixture(dataSource);
    try (Connection connection = dataSource.getConnection()) {
      fixture.execute(
          connection,
          "insert into stores (id, name, code, created_at, updated_at) values (?, ?, ?, ?, ?)",
          fixture.storeId,
          "MCM HAUS",
          "mcm-haus-" + fixture.storeId,
          TIMESTAMP,
          TIMESTAMP);
      fixture.execute(
          connection,
          "insert into customers (id, name, token_hash, created_at, updated_at) values (?, ?, ?, ?, ?)",
          fixture.customerId,
          null,
          "customer-token-hash-" + fixture.customerId,
          TIMESTAMP,
          TIMESTAMP);
      fixture.execute(
          connection,
          "insert into staff (id, store_id, name, profile_image_url, active, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?)",
          fixture.staffId,
          fixture.storeId,
          "김회윤",
          null,
          true,
          TIMESTAMP,
          TIMESTAMP);
      fixture.execute(
          connection,
          "insert into store_devices (id, store_id, selected_staff_id, name, token_hash, active, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
          fixture.deviceId,
          fixture.storeId,
          fixture.staffId,
          "1층 태블릿",
          "device-token-hash-" + fixture.deviceId,
          true,
          TIMESTAMP,
          TIMESTAMP);
      fixture.execute(
          connection,
          "insert into visits (id, customer_id, store_id, staff_id, store_device_id, waiting_number, service_language, interaction_style, additional_request, status, arc_creation_granted_at, matched_at, completed_at, canceled_at, version, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          fixture.visitId,
          fixture.customerId,
          fixture.storeId,
          fixture.staffId,
          fixture.deviceId,
          fixture.waitingNumber,
          "KO",
          "STAFF_RECOMMENDATION",
          null,
          "ARC_IN_PROGRESS",
          TIMESTAMP,
          TIMESTAMP,
          null,
          null,
          0L,
          TIMESTAMP,
          TIMESTAMP);
    }
    return fixture;
  }

  void insertArc(String arcId) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      execute(
          connection,
          "insert into arcs (id, visit_id, customer_id, image_object_key, status, created_by_staff_id, last_modified_by_staff_id, confirmed_at, finalized_at, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          arcId,
          visitId,
          customerId,
          "arc/final.png",
          "DRAFT",
          staffId,
          staffId,
          null,
          null,
          TIMESTAMP,
          TIMESTAMP);
    }
  }

  void insertArcForUnknownVisit() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      execute(
          connection,
          "insert into arcs (id, visit_id, customer_id, image_object_key, status, created_by_staff_id, last_modified_by_staff_id, confirmed_at, finalized_at, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          "00000000-0000-0000-0000-000000000103",
          UUID.randomUUID().toString(),
          customerId,
          "arc/unknown.png",
          "DRAFT",
          staffId,
          staffId,
          null,
          null,
          TIMESTAMP,
          TIMESTAMP);
    }
  }

  private void execute(Connection connection, String sql, Object... values) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < values.length; index++) {
        statement.setObject(index + 1, values[index]);
      }
      statement.executeUpdate();
    }
  }
}
