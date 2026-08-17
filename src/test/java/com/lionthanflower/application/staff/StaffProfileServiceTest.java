// 직원 프로필 등록 서비스의 토큰 중복과 신규 등록 규칙을 검증하는 테스트
package com.lionthanflower.application.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionthanflower.application.staff.dto.StaffProfileRegisterRequest;
import com.lionthanflower.application.staff.dto.StaffRegistrationResult;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.error.StaffErrorCode;
import com.lionthanflower.domain.store.repository.StaffRepository;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
import com.lionthanflower.infrastructure.security.StaffTokenGenerator;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffProfileServiceTest {

  @Mock private StaffRepository staffRepository;
  @Mock private StoreRepository storeRepository;
  @Mock private StaffTokenGenerator staffTokenGenerator;

  private StaffProfileService service;
  private UUID storeId;
  private StaffProfileRegisterRequest request;

  @BeforeEach
  void setUp() {
    service = new StaffProfileService(staffRepository, storeRepository, staffTokenGenerator);
    storeId = UUID.randomUUID();
    request = new StaffProfileRegisterRequest(storeId, "김형진", Set.of("EN", "JA"));
  }

  @Test
  void 유효한_기존_토큰이_있으면_중복_등록을_거부한다() {
    when(staffTokenGenerator.hash("existing-token")).thenReturn("existing-hash");
    when(staffRepository.existsByTokenHash("existing-hash")).thenReturn(true);

    assertThatThrownBy(() -> service.register(request, "existing-token"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(StaffErrorCode.PROFILE_ALREADY_EXISTS);
  }

  @Test
  void 알_수_없는_토큰은_신규_등록을_허용하고_새_토큰을_발급한다() {
    when(staffTokenGenerator.hash("unknown-token")).thenReturn("unknown-hash");
    when(staffRepository.existsByTokenHash("unknown-hash")).thenReturn(false);
    when(storeRepository.existsById(storeId)).thenReturn(true);
    when(staffTokenGenerator.generateRawToken()).thenReturn("issued-token");
    when(staffTokenGenerator.hash("issued-token")).thenReturn("issued-hash");
    when(staffRepository.save(any(Staff.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    StaffRegistrationResult result = service.register(request, "unknown-token");

    assertThat(result.rawToken()).isEqualTo("issued-token");
    assertThat(result.profile().storeId()).isEqualTo(storeId);
    assertThat(result.profile().languages())
        .containsExactlyInAnyOrder(LanguageCode.EN, LanguageCode.JA);
    verify(staffRepository).save(any(Staff.class));
  }
}
