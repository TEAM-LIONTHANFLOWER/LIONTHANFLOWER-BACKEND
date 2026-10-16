// 여러 익명 고객의 보관 기록을 연결하는 이메일 계정 엔티티
package com.lionthanflower.domain.customer.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customer_email_identities")
public class CustomerEmailIdentity extends BaseEntity {
  @JdbcTypeCode(SqlTypes.CHAR)
  @jakarta.persistence.Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  protected CustomerEmailIdentity() {}

  @PrePersist
  @PreUpdate
  private void validateEmail() {
    email = EmailAddress.normalize(email);
  }

  public static CustomerEmailIdentity create(String email) {
    CustomerEmailIdentity identity = new CustomerEmailIdentity();
    identity.id = UUID.randomUUID();
    identity.email = EmailAddress.normalize(email);
    return identity;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }
}
