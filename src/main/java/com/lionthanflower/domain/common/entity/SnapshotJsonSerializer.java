// 검증된 도메인 입력 스냅샷을 JSON 문자열로 직렬화하는 도구
package com.lionthanflower.domain.common.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SnapshotJsonSerializer {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private SnapshotJsonSerializer() {}

  public static String serialize(Object snapshot) {
    if (snapshot == null) {
      throw new IllegalArgumentException("입력 스냅샷은 null일 수 없습니다.");
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(snapshot);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("입력 스냅샷을 JSON으로 변환할 수 없습니다.", exception);
    }
  }
}
