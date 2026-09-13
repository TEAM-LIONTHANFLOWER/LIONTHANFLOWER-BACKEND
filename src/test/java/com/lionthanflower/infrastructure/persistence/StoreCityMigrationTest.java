// 기존 매장 데이터에 도시 컬럼을 추가하는 양쪽 DB의 순방향 마이그레이션을 검증합니다.
package com.lionthanflower.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

class StoreCityMigrationTest {

  @ParameterizedTest
  @ValueSource(strings = {"postgres", "mysql"})
  void 기존_매장을_보존하고_확인된_서울_매장만_도시를_보완한다(String databaseType) throws Exception {
    try (JdbcDatabaseContainer<?> database = createDatabase(databaseType)) {
      database.start();
      var configuration =
          Flyway.configure()
              .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
              .locations("classpath:db/migration");
      configuration.target("7").load().migrate();
      String legacyId = UUID.randomUUID().toString();
      try (var connection =
              DriverManager.getConnection(
                  database.getJdbcUrl(), database.getUsername(), database.getPassword());
          var insert =
              connection.prepareStatement(
                  "INSERT INTO stores (id, name, code, country_code, created_at, updated_at) "
                      + "VALUES (?, 'MCM Seoul', 'LEGACY-UNKNOWN', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
        insert.setString(1, legacyId);
        insert.executeUpdate();
      }

      var flyway = configuration.target("8").load();
      assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
      assertThat(flyway.migrate().migrationsExecuted).isZero();
      try (var connection =
              DriverManager.getConnection(
                  database.getJdbcUrl(), database.getUsername(), database.getPassword());
          var statement =
              connection.prepareStatement(
                  "SELECT id, name, country_code, city_code FROM stores WHERE code = ?")) {
        statement.setString(1, "MCM-SEOUL");
        try (var rows = statement.executeQuery()) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString("city_code")).isEqualTo("SEOUL");
          assertThat(rows.getString("country_code")).isEqualTo("KR");
        }
        statement.setString(1, "LEGACY-UNKNOWN");
        try (var rows = statement.executeQuery()) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString("id")).isEqualTo(legacyId);
          assertThat(rows.getString("name")).isEqualTo("MCM Seoul");
          assertThat(rows.getString("country_code")).isEqualTo("KR");
          assertThat(rows.getString("city_code")).isNull();
        }
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"postgres", "mysql"})
  void 파리와_뮌헨_매장을_고정_도시_코드로_추가한다(String databaseType) throws Exception {
    try (JdbcDatabaseContainer<?> database = createDatabase(databaseType)) {
      database.start();
      var configuration =
          Flyway.configure()
              .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
              .locations("classpath:db/migration");
      configuration.target("8").load().migrate();

      var flyway = configuration.target("9").load();
      assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
      assertStore(database, "MCM-PARIS", "MCM Paris", "FR", "PARIS");
      assertStore(database, "MCM-MUNICH", "MCM Munich", "DE", "MUNICH");
      assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"postgres", "mysql"})
  void 동일한_코드의_기존_매장은_보존한다(String databaseType) throws Exception {
    try (JdbcDatabaseContainer<?> database = createDatabase(databaseType)) {
      database.start();
      var configuration =
          Flyway.configure()
              .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
              .locations("classpath:db/migration");
      configuration.target("8").load().migrate();
      String legacyId = UUID.randomUUID().toString();
      try (var connection =
              DriverManager.getConnection(
                  database.getJdbcUrl(), database.getUsername(), database.getPassword());
          var insert =
              connection.prepareStatement(
                  "INSERT INTO stores (id, name, code, country_code, city_code, created_at, updated_at) "
                      + "VALUES (?, 'Legacy Paris', 'MCM-PARIS', 'FR', 'LEGACY_PARIS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
        insert.setString(1, legacyId);
        insert.executeUpdate();
      }

      configuration.target("9").load().migrate();

      assertThat(countStoresByCode(database, "MCM-PARIS")).isEqualTo(1);
      assertStore(database, "MCM-PARIS", "Legacy Paris", "FR", "LEGACY_PARIS");
      assertStore(database, "MCM-MUNICH", "MCM Munich", "DE", "MUNICH");
    }
  }

  private JdbcDatabaseContainer<?> createDatabase(String databaseType) {
    return databaseType.equals("postgres")
        ? new PostgreSQLContainer<>("postgres:17-alpine")
        : new MySQLContainer<>("mysql:8.4");
  }

  private void assertStore(
      JdbcDatabaseContainer<?> database,
      String code,
      String name,
      String countryCode,
      String cityCode)
      throws Exception {
    try (var connection =
            DriverManager.getConnection(
                database.getJdbcUrl(), database.getUsername(), database.getPassword());
        var statement =
            connection.prepareStatement(
                "SELECT name, country_code, city_code FROM stores WHERE code = ?")) {
      statement.setString(1, code);
      try (var rows = statement.executeQuery()) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getString("name")).isEqualTo(name);
        assertThat(rows.getString("country_code")).isEqualTo(countryCode);
        assertThat(rows.getString("city_code")).isEqualTo(cityCode);
      }
    }
  }

  private int countStoresByCode(JdbcDatabaseContainer<?> database, String code) throws Exception {
    try (var connection =
            DriverManager.getConnection(
                database.getJdbcUrl(), database.getUsername(), database.getPassword());
        var statement = connection.prepareStatement("SELECT COUNT(*) FROM stores WHERE code = ?")) {
      statement.setString(1, code);
      try (var rows = statement.executeQuery()) {
        rows.next();
        return rows.getInt(1);
      }
    }
  }
}
