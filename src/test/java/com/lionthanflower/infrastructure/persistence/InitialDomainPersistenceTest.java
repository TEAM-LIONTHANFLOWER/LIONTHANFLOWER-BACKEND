// IA 기준 도메인 스키마와 JPA 매핑의 일치 여부를 검증하는 통합 테스트
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
  void IA_도메인_테이블이_생성되고_공용_단말_테이블은_제거된다() throws SQLException {
    assertThat(
            countTables(
                "stores",
                "customers",
                "staff",
                "staff_languages",
                "visits",
                "products",
                "product_variants",
                "purchases",
                "purchase_items",
                "arcs",
                "arc_revisions",
                "visit_memories",
                "myself_images"))
        .isEqualTo(13);
    assertThat(countTables("store_devices")).isZero();
  }

  @Test
  void 고객별_Arc_번호_컬럼과_유일_제약이_생성된다() throws SQLException {
    assertThat(countColumns("arcs", "arc_number")).isEqualTo(1);
    assertThat(countConstraints("arcs", "uk_arcs_customer_arc_number")).isEqualTo(1);
  }

  @Test
  void 방문당_구매와_Arc와_Visit_Memory는_각각_하나만_저장할_수_있다() throws SQLException {
    PersistenceFixture fixture = PersistenceFixture.insertRequiredRows(dataSource);

    fixture.insertPurchase();
    assertThatThrownBy(fixture::insertDuplicatePurchaseForVisit).isInstanceOf(SQLException.class);

    fixture.insertArc();

    PersistenceFixture noPurchaseFixture = PersistenceFixture.insertNoPurchaseRows(dataSource);
    noPurchaseFixture.insertVisitMemory();
    assertThatThrownBy(noPurchaseFixture::insertVisitMemory).isInstanceOf(SQLException.class);
  }

  @Test
  void 같은_Arc의_리비전_번호는_중복될_수_없다() throws SQLException {
    PersistenceFixture fixture = PersistenceFixture.insertRequiredRows(dataSource);
    fixture.insertPurchase();
    fixture.insertArc();
    fixture.insertArcRevision(1);

    assertThatThrownBy(() -> fixture.insertArcRevision(1)).isInstanceOf(SQLException.class);
  }

  @Test
  void 다른_Arc의_리비전을_공유_리비전으로_참조할_수_없다() throws SQLException {
    PersistenceFixture fixture = PersistenceFixture.insertRequiredRows(dataSource);
    fixture.insertPurchase();
    fixture.insertArc();

    PersistenceFixture other = PersistenceFixture.insertRequiredRows(dataSource);
    other.insertPurchase();
    other.insertArc();
    String otherRevisionId = other.insertArcRevision(1);

    assertThatThrownBy(() -> fixture.updateSharedRevision(otherRevisionId))
        .isInstanceOf(SQLException.class);
  }

  @Test
  void Arc의_방문과_구매_유일_제약을_각각_검증한다() throws SQLException {
    PersistenceFixture fixture = PersistenceFixture.insertRequiredRows(dataSource);
    fixture.insertPurchase();
    fixture.insertArc();

    PersistenceFixture otherPurchase = PersistenceFixture.insertRequiredRows(dataSource);
    otherPurchase.insertPurchase();
    assertThatThrownBy(() -> fixture.insertArcWithPurchase(otherPurchase.getPurchaseId()))
        .isInstanceOf(SQLException.class);

    PersistenceFixture otherVisit = PersistenceFixture.insertRequiredRows(dataSource);
    assertThatThrownBy(() -> fixture.insertArcForVisit(otherVisit.getVisitId()))
        .isInstanceOf(SQLException.class);
  }

  private int countTables(String... tableNames) throws SQLException {
    String placeholders = String.join(",", java.util.Collections.nCopies(tableNames.length, "?"));
    String sql =
        "select count(*) from information_schema.tables where table_schema = 'public' "
            + "and table_name in ("
            + placeholders
            + ")";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < tableNames.length; index++) {
        statement.setString(index + 1, tableNames[index]);
      }
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int countColumns(String tableName, String columnName) throws SQLException {
    String sql =
        "select count(*) from information_schema.columns "
            + "where table_schema = 'public' and table_name = ? and column_name = ?";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, tableName);
      statement.setString(2, columnName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private int countConstraints(String tableName, String constraintName) throws SQLException {
    String sql =
        "select count(*) from information_schema.table_constraints "
            + "where table_schema = 'public' and table_name = ? and constraint_name = ?";
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, tableName);
      statement.setString(2, constraintName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }
}

final class PersistenceFixture {

  private static final Timestamp TIMESTAMP = Timestamp.valueOf("2026-08-15 18:00:00");

  private final DataSource dataSource;
  private final String storeId = UUID.randomUUID().toString();
  private final String customerId = UUID.randomUUID().toString();
  private final String staffId = UUID.randomUUID().toString();
  private final String visitId = UUID.randomUUID().toString();
  private final String purchaseId = UUID.randomUUID().toString();
  private final String arcId = UUID.randomUUID().toString();

  private PersistenceFixture(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  static PersistenceFixture insertRequiredRows(DataSource dataSource) throws SQLException {
    PersistenceFixture fixture = new PersistenceFixture(dataSource);
    fixture.insertBaseRows("ARC_IN_PROGRESS", "PURCHASED");
    return fixture;
  }

  static PersistenceFixture insertNoPurchaseRows(DataSource dataSource) throws SQLException {
    PersistenceFixture fixture = new PersistenceFixture(dataSource);
    fixture.insertBaseRows("VISIT_MEMORY_IN_PROGRESS", "NOT_PURCHASED");
    return fixture;
  }

  void insertPurchase() throws SQLException {
    insertPurchase(purchaseId);
  }

  void insertDuplicatePurchaseForVisit() throws SQLException {
    insertPurchase(UUID.randomUUID().toString());
  }

  private void insertPurchase(String id) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      execute(
          connection,
          "insert into purchases (id, visit_id, created_at, updated_at) values (?, ?, ?, ?)",
          id,
          visitId,
          TIMESTAMP,
          TIMESTAMP);
    }
  }

  void insertArc() throws SQLException {
    insertArc(arcId);
  }

  void insertArcWithPurchase(String otherPurchaseId) throws SQLException {
    insertArc(UUID.randomUUID().toString(), visitId, otherPurchaseId);
  }

  void insertArcForVisit(String otherVisitId) throws SQLException {
    insertArc(UUID.randomUUID().toString(), otherVisitId, purchaseId);
  }

  private void insertArc(String id) throws SQLException {
    insertArc(id, visitId, purchaseId);
  }

  private void insertArc(String id, String targetVisitId, String targetPurchaseId)
      throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      execute(
          connection,
          "insert into arcs (id, visit_id, purchase_id, customer_id, created_by_staff_id, status,"
              + " created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
          id,
          targetVisitId,
          targetPurchaseId,
          customerId,
          staffId,
          "DRAFT",
          TIMESTAMP,
          TIMESTAMP);
    }
  }

  String getVisitId() {
    return visitId;
  }

  String getPurchaseId() {
    return purchaseId;
  }

  void insertVisitMemory() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      execute(
          connection,
          "insert into visit_memories (id, visit_id, customer_id, created_by_staff_id,"
              + " input_snapshot, template_version, status, created_at, updated_at) values (?, ?,"
              + " ?, ?, ?, ?, ?, ?, ?)",
          UUID.randomUUID().toString(),
          visitId,
          customerId,
          staffId,
          "{\"products\":[]}",
          "visit-memory-v1",
          "DRAFT",
          TIMESTAMP,
          TIMESTAMP);
    }
  }

  String insertArcRevision(int revisionNumber) throws SQLException {
    String revisionId = UUID.randomUUID().toString();
    try (Connection connection = dataSource.getConnection()) {
      execute(
          connection,
          "insert into arc_revisions (id, arc_id, revision_number, input_snapshot,"
              + " template_version, status, created_by_staff_id, created_at, updated_at) values (?,"
              + " ?, ?, ?, ?, ?, ?, ?, ?)",
          revisionId,
          arcId,
          revisionNumber,
          "{\"schemaVersion\":1}",
          "arc-v1",
          "GENERATING",
          staffId,
          TIMESTAMP,
          TIMESTAMP);
    }
    return revisionId;
  }

  void updateSharedRevision(String revisionId) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      execute(
          connection,
          "update arcs set shared_revision_id = ?, status = ? where id = ?",
          revisionId,
          "SHARED",
          arcId);
    }
  }

  private void insertBaseRows(String visitStatus, String purchaseDecision) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      execute(
          connection,
          "insert into stores (id, name, code, country_code, created_at, updated_at) values (?, ?,"
              + " ?, ?, ?, ?)",
          storeId,
          "MCM HAUS",
          "mcm-haus-" + storeId,
          "KR",
          TIMESTAMP,
          TIMESTAMP);
      execute(
          connection,
          "insert into customers (id, name, token_hash, created_at, updated_at) values (?, ?, ?, ?,"
              + " ?)",
          customerId,
          null,
          "customer-token-hash-" + customerId,
          TIMESTAMP,
          TIMESTAMP);
      execute(
          connection,
          "insert into staff (id, store_id, name, token_hash, created_at, updated_at) values (?, ?,"
              + " ?, ?, ?, ?)",
          staffId,
          storeId,
          "김회윤",
          "staff-token-hash-" + staffId,
          TIMESTAMP,
          TIMESTAMP);
      execute(
          connection,
          "insert into staff_languages (staff_id, language) values (?, ?)",
          staffId,
          "EN");
      execute(
          connection,
          "insert into visits (id, customer_id, store_id, staff_id, service_language,"
              + " interaction_style, additional_request, status, purchase_decision,"
              + " purchase_decided_by_staff_id, purchase_decided_at, matched_at, version,"
              + " created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          visitId,
          customerId,
          storeId,
          staffId,
          "EN",
          "STAFF_RECOMMENDATION",
          null,
          visitStatus,
          purchaseDecision,
          staffId,
          TIMESTAMP,
          TIMESTAMP,
          0L,
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
