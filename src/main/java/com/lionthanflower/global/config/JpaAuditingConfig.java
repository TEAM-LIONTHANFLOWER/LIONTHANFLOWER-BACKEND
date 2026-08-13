// JPA 엔티티의 생성 및 수정 시각 자동 기록을 활성화하는 설정
package com.lionthanflower.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
