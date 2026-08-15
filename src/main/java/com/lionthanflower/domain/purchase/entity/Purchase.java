// 구매 확정 방문의 구매 집계를 관리하는 엔티티
package com.lionthanflower.domain.purchase.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "purchases",
    uniqueConstraints = @UniqueConstraint(name = "uk_purchases_visit_id", columnNames = "visit_id"))
public class Purchase extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "visit_id", nullable = false, unique = true, length = 36)
  private UUID visitId;

  protected Purchase() {}

  private Purchase(UUID id, UUID visitId) {
    this.id = id;
    this.visitId = visitId;
  }

  public static Purchase create(UUID visitId) {
    if (visitId == null) {
      throw new IllegalArgumentException("방문 ID는 null일 수 없습니다.");
    }
    return new Purchase(UUID.randomUUID(), visitId);
  }

  public UUID getId() {
    return id;
  }

  public UUID getVisitId() {
    return visitId;
  }
}
