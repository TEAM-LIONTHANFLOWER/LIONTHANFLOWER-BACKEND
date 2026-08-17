// 실제 staffToken 쿠키로 직원 프로필 조회와 언어 로딩을 검증하는 통합 테스트
package com.lionthanflower.controller.staff;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.infrastructure.security.StaffTokenGenerator;
import com.lionthanflower.support.PostgreSqlContainerSupport;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StaffProfileControllerCookieIntegrationTest extends PostgreSqlContainerSupport {

  private static final Timestamp TIMESTAMP = Timestamp.from(Instant.parse("2026-08-18T00:00:00Z"));

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private StaffTokenGenerator staffTokenGenerator;

  @Test
  void staffToken_쿠키로_조회하면_직원_프로필과_언어를_반환한다() throws Exception {
    UUID storeId = UUID.randomUUID();
    UUID staffId = UUID.randomUUID();
    String rawToken = "staff-token-for-profile-inquiry";

    insertStore(storeId);
    insertStaff(staffId, storeId, staffTokenGenerator.hash(rawToken));
    insertLanguage(staffId, "EN");
    insertLanguage(staffId, "JA");

    mockMvc
        .perform(get("/api/staff/me/profile").cookie(new Cookie("staffToken", rawToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.staffId").value(staffId.toString()))
        .andExpect(jsonPath("$.data.name").value("김형진"))
        .andExpect(jsonPath("$.data.languages", hasItems("EN", "JA")));
  }

  private void insertStore(UUID storeId) {
    jdbcTemplate.update(
        "insert into stores (id, name, code, country_code, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?)",
        storeId,
        "MCM HAUS",
        "mcm-haus-" + storeId,
        "KR",
        TIMESTAMP,
        TIMESTAMP);
  }

  private void insertStaff(UUID staffId, UUID storeId, String tokenHash) {
    jdbcTemplate.update(
        "insert into staff (id, store_id, name, token_hash, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?)",
        staffId,
        storeId,
        "김형진",
        tokenHash,
        TIMESTAMP,
        TIMESTAMP);
  }

  private void insertLanguage(UUID staffId, String language) {
    jdbcTemplate.update(
        "insert into staff_languages (staff_id, language) values (?, ?)", staffId, language);
  }
}
