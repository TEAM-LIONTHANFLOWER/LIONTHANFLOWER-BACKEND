// 직원 등록 전에 사용할 매장 검색을 처리하는 Application Service
package com.lionthanflower.application.store;

import com.lionthanflower.domain.store.entity.Store;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StoreQueryService {

  private final StoreRepository storeRepository;

  public StoreQueryService(StoreRepository storeRepository) {
    this.storeRepository = storeRepository;
  }

  public List<StoreSummary> search(String query) {
    String normalizedQuery = query == null ? "" : query.trim();
    return storeRepository
        .findTop20ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(
            normalizedQuery, normalizedQuery)
        .stream()
        .map(StoreSummary::from)
        .toList();
  }

  public record StoreSummary(UUID storeId, String name, String code, String countryCode) {

    private static StoreSummary from(Store store) {
      return new StoreSummary(
          store.getId(), store.getName(), store.getCode(), store.getCountryCode());
    }
  }
}
