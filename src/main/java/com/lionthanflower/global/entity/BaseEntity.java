// 모든 도메인 엔티티의 생성 및 수정 시각을 관리하는 JPA 기반 클래스
package com.lionthanflower.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

  @CreatedDate
  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected BaseEntity() {}

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
