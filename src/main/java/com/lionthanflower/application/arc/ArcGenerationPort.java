// Arc 생성 외부 연동을 추상화하는 Application Port
package com.lionthanflower.application.arc;

import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;

public interface ArcGenerationPort {
  ArcGeneratedContent generate(ArcGenerationCommand command);
}
