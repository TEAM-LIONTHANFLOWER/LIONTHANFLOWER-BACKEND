// 매장 국가와 직원 개인 기기 프로필 생성 규칙을 검증하는 테스트
package com.lionthanflower.domain.store.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionthanflower.domain.common.entity.LanguageCode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreDomainTest {

  @Test
  void 매장과_개인_기기_직원_프로필을_생성한다() {
    Store store = Store.create("MCM HAUS", "mcm-haus", "kr");
    Staff staff =
        Staff.create(
            store.getId(), "김회윤", "staff-token-hash", Set.of(LanguageCode.EN, LanguageCode.JA));

    assertThat(store.getCountryCode()).isEqualTo("KR");
    assertThat(staff.getStoreId()).isEqualTo(store.getId());
    assertThat(staff.getTokenHash()).isEqualTo("staff-token-hash");
    assertThat(staff.getLanguages()).containsExactlyInAnyOrder(LanguageCode.EN, LanguageCode.JA);
  }

  @Test
  void 직원은_하나_이상의_구사_언어가_필요하다() {
    assertThatThrownBy(() -> Staff.create(UUID.randomUUID(), "김회윤", "staff-token-hash", Set.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("직원 구사 언어는 하나 이상이어야 합니다.");
  }

  @Test
  void 직원_구사_언어_집합은_null일_수_없다() {
    assertThatThrownBy(() -> Staff.create(UUID.randomUUID(), "김회윤", "staff-token-hash", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("직원 구사 언어는 하나 이상이어야 합니다.");
  }

  @Test
  void 직원_구사_언어에는_null이_포함될_수_없다() {
    Set<LanguageCode> languages = new HashSet<>(Arrays.asList(LanguageCode.EN, null));

    assertThatThrownBy(() -> Staff.create(UUID.randomUUID(), "김회윤", "staff-token-hash", languages))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("직원 구사 언어는 하나 이상이어야 합니다.");
  }

  @Test
  void 국가_코드는_두_글자여야_한다() {
    assertThatThrownBy(() -> Store.create("MCM HAUS", "mcm-haus", "KOR"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("매장 국가 코드는 ISO alpha-2 형식이어야 합니다.");
  }

  @Test
  void 존재하지_않는_ISO_국가_코드는_거부한다() {
    assertThatThrownBy(() -> Store.create("MCM HAUS", "mcm-haus", "ZZ"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("매장 국가 코드는 ISO alpha-2 형식이어야 합니다.");
    assertThatThrownBy(() -> Store.create("MCM HAUS", "mcm-haus", "1!"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("매장 국가 코드는 ISO alpha-2 형식이어야 합니다.");
  }
}
