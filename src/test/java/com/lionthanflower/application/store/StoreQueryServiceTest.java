// 이름과 코드로 매장을 검색하는 Application Service를 검증하는 테스트
package com.lionthanflower.application.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lionthanflower.domain.store.entity.Store;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreQueryServiceTest {

  @Mock private StoreRepository storeRepository;

  private StoreQueryService service;

  @BeforeEach
  void setUp() {
    service = new StoreQueryService(storeRepository);
  }

  @Test
  void 검색어를_정규화해_이름과_코드로_매장을_검색한다() {
    Store store = Store.create("MCM Seoul", "MCM-SEOUL", "KR");
    when(storeRepository
            .findTop20ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(
                "seoul", "seoul"))
        .thenReturn(List.of(store));

    List<StoreQueryService.StoreSummary> result = service.search(" seoul ");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().storeId()).isEqualTo(store.getId());
    assertThat(result.getFirst().name()).isEqualTo("MCM Seoul");
    assertThat(result.getFirst().code()).isEqualTo("MCM-SEOUL");
    assertThat(result.getFirst().countryCode()).isEqualTo("KR");
  }
}
