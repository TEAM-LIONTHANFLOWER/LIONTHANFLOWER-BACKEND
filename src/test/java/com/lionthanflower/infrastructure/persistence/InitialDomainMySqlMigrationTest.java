// MySQL에서 IA migration과 JPA 매핑의 호환성을 검증하는 테스트
package com.lionthanflower.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionthanflower.support.MySqlContainerSupport;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InitialDomainMySqlMigrationTest extends MySqlContainerSupport {

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

  private int countTables(String... tableNames) throws SQLException {
    String placeholders = String.join(",", java.util.Collections.nCopies(tableNames.length, "?"));
    String sql =
        "select count(*) from information_schema.tables where table_schema = database() "
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
}
