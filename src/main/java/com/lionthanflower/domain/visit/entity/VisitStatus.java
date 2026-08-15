// 방문의 온보딩과 매칭 진행 상태를 정의하는 enum
package com.lionthanflower.domain.visit.entity;

public enum VisitStatus {
  ONBOARDING,
  WAITING_FOR_STAFF,
  ACTIVE,
  ARC_IN_PROGRESS,
  VISIT_MEMORY_IN_PROGRESS,
  COMPLETED,
  CANCELED
}
