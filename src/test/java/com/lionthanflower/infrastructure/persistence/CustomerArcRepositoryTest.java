// 고객 공개 Arc 조회 Repository의 메서드 계약을 검증하는 단위 테스트
package com.lionthanflower.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lionthanflower.domain.arc.entity.ArcStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerArcRepositoryTest {

  @Mock private ArcRepository arcRepository;

  @Test
  void 고객별_공개_Arc_목록과_상세_소유권_조회_메서드를_제공한다() {
    UUID customerId = UUID.randomUUID();
    UUID arcId = UUID.randomUUID();
    List<ArcStatus> visibleStatuses = List.of(ArcStatus.SHARED, ArcStatus.FINALIZED);
    when(arcRepository.findByCustomerIdAndStatusInOrderByArcNumberDesc(customerId, visibleStatuses))
        .thenReturn(List.of());

    assertThat(
            arcRepository.findByCustomerIdAndStatusInOrderByArcNumberDesc(
                customerId, visibleStatuses))
        .isEmpty();
    assertThat(arcRepository.findByIdAndCustomerIdAndStatusIn(arcId, customerId, visibleStatuses))
        .isEmpty();
  }
}
