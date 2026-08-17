// 고객 Arc 생성 콘텐츠의 구조화 JSON 파싱과 필수 값 검증을 테스트하는 단위 테스트
package com.lionthanflower.domain.arc.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionthanflower.domain.common.entity.SnapshotJsonSerializer;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArcGeneratedContentTest {

  @Test
  void 구조화된_Arc_생성_JSON을_검증된_객체로_읽는다() {
    ArcGeneratedContent content =
        SnapshotJsonSerializer.deserialize(
            """
            {"momentSummary":"균형을 중요하게 생각합니다.","preferences":["실용적인 디자인"],"momentToRemember":"수납공간을 오래 고민했습니다."}
            """,
            ArcGeneratedContent.class);

    assertThat(content.momentSummary()).isEqualTo("균형을 중요하게 생각합니다.");
    assertThat(content.preferences()).containsExactly("실용적인 디자인");
    assertThat(content.momentToRemember()).isEqualTo("수납공간을 오래 고민했습니다.");
  }

  @Test
  void 선호_문장이_비어_있으면_생성_콘텐츠를_만들_수_없다() {
    assertThatThrownBy(() -> new ArcGeneratedContent("요약", List.of(), "기억"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("고객 선호는 하나 이상이어야 합니다.");
  }
}
