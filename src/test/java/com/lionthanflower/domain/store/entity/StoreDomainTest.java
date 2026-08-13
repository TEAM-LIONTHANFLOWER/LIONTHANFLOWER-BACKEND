// 매장과 직원 단말의 생성 및 상태 변경 규칙을 검증하는 테스트
package com.lionthanflower.domain.store.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StoreDomainTest {

  @Test
  void 매장과_직원은_활성_상태로_생성된다() {
    Store store = Store.create("MCM HAUS", "mcm-haus");
    Staff staff = Staff.create(store.getId(), "김회윤", null);

    assertThat(store.getId()).isNotNull();
    assertThat(store.getName()).isEqualTo("MCM HAUS");
    assertThat(staff.getStoreId()).isEqualTo(store.getId());
    assertThat(staff.isActive()).isTrue();
  }

  @Test
  void 단말은_직원을_선택하고_비활성화할_수_있다() {
    UUID storeId = UUID.randomUUID();
    UUID staffId = UUID.randomUUID();
    StoreDevice device = StoreDevice.create(storeId, "1층 태블릿", "device-token-hash");

    device.selectStaff(staffId);
    device.deactivate();

    assertThat(device.getSelectedStaffId()).isEqualTo(staffId);
    assertThat(device.isActive()).isFalse();
  }

  @Test
  void 비활성화된_단말은_직원을_선택할_수_없다() {
    StoreDevice device = StoreDevice.create(UUID.randomUUID(), "1층 태블릿", "device-token-hash");
    device.deactivate();

    assertThatThrownBy(() -> device.selectStaff(UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("비활성화된 단말에서는 직원을 선택할 수 없습니다.");
  }
}
