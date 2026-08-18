// Visit Memory 생성 외부 연동을 추상화하는 Application Port
package com.lionthanflower.application.visitmemory;

import com.lionthanflower.domain.visitmemory.entity.VisitMemoryGeneratedContent;

public interface VisitMemoryGenerationPort {
  VisitMemoryGeneratedContent generate(VisitMemoryGenerationCommand command);
}
