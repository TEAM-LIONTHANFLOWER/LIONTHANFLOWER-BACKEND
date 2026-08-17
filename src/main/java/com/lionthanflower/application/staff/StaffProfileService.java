// 직원 프로필 등록을 처리하는 서비스
package com.lionthanflower.application.staff;

import com.lionthanflower.application.staff.dto.StaffProfileRegisterRequest;
import com.lionthanflower.application.staff.dto.StaffProfileResponse;
import com.lionthanflower.application.staff.dto.StaffRegistrationResult;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.error.StaffErrorCode;
import com.lionthanflower.domain.store.repository.StaffRepository;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
import com.lionthanflower.infrastructure.security.StaffTokenGenerator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffProfileService {
  private final StaffRepository staffRepository;
  private final StoreRepository storeRepository;
  private final StaffTokenGenerator staffTokenGenerator;

  public StaffProfileService(
      StaffRepository staffRepository,
      StoreRepository storeRepository,
      StaffTokenGenerator staffTokenGenerator) {
    this.staffRepository = staffRepository;
    this.storeRepository = storeRepository;
    this.staffTokenGenerator = staffTokenGenerator;
  }

  @Transactional
  public StaffRegistrationResult register(
      StaffProfileRegisterRequest request, String existingRawToken) {
    if (existingRawToken != null
        && staffRepository.existsByTokenHash(staffTokenGenerator.hash(existingRawToken))) {
      throw new BusinessException(StaffErrorCode.PROFILE_ALREADY_EXISTS);
    }
    if (!storeRepository.existsById(request.storeId())) {
      throw new BusinessException(StaffErrorCode.INVALID_STORE_ID);
    }

    Set<LanguageCode> languages = parseLanguages(request.languages());

    String rawToken = staffTokenGenerator.generateRawToken();
    String tokenHash = staffTokenGenerator.hash(rawToken);

    Staff staff = Staff.create(request.storeId(), request.name(), tokenHash, languages);
    staffRepository.save(staff);

    return new StaffRegistrationResult(StaffProfileResponse.from(staff), rawToken);
  }

  private Set<LanguageCode> parseLanguages(Set<String> rawLanguages) {
    Set<LanguageCode> languages = new HashSet<>();
    for (String rawLanguage : rawLanguages) {
      try {
        languages.add(LanguageCode.valueOf(rawLanguage.toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException exception) {
        throw new BusinessException(StaffErrorCode.INVALID_LANGUAGE_CODE);
      }
    }
    return languages;
  }
}
