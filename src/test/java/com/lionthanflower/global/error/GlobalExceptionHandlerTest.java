// 전역 예외 처리기의 HTTP 상태와 오류 응답 변환을 검증하는 테스트
package com.lionthanflower.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void 비즈니스_예외를_오류_코드의_HTTP_응답으로_변환한다() throws Exception {
    mockMvc
        .perform(get("/test/business"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("TEST-409"))
        .andExpect(jsonPath("$.error.message").value("테스트 충돌이 발생했습니다."))
        .andExpect(jsonPath("$.error.fieldErrors").isEmpty());
  }

  @Test
  void 검증_실패를_필드_오류로_변환하고_거절된_값은_노출하지_않는다() throws Exception {
    mockMvc
        .perform(
            post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"secret-name\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"))
        .andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"))
        .andExpect(jsonPath("$.error.fieldErrors[0].message").value("이름은 세 글자 이하여야 합니다."))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret-name"))));
  }

  @Test
  void 읽을_수_없는_JSON을_공통_400_응답으로_변환한다() throws Exception {
    mockMvc
        .perform(
            post("/test/validation").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"));
  }

  @Test
  void 필수_요청_파라미터_누락을_공통_400_응답으로_변환한다() throws Exception {
    mockMvc
        .perform(get("/test/parameter"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"));
  }

  @Test
  void 메서드_파라미터_검증_실패를_공통_400_응답으로_변환한다() throws Exception {
    mockMvc
        .perform(get("/test/parameter-validation").param("age", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"))
        .andExpect(jsonPath("$.error.fieldErrors[0].field").value("age"))
        .andExpect(jsonPath("$.error.fieldErrors[0].message").value("나이는 1 이상이어야 합니다."));
  }

  @Test
  void 모델_바인딩_검증_실패를_공통_400_응답으로_변환한다() throws Exception {
    mockMvc
        .perform(get("/test/model-validation").param("name", "long-name"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"))
        .andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"))
        .andExpect(jsonPath("$.error.fieldErrors[0].message").value("이름은 세 글자 이하여야 합니다."));
  }

  @Test
  void 모델_바인딩_타입_변환_실패를_공통_400_응답으로_변환한다() throws Exception {
    mockMvc
        .perform(get("/test/model-binding").param("count", "not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("not-a-number"))));
  }

  @Test
  void 존재하지_않는_경로를_공통_404_응답으로_변환한다() throws Exception {
    mockMvc
        .perform(get("/unknown"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("COMMON-404"));
  }

  @Test
  void 지원하지_않는_HTTP_메서드를_공통_405_응답으로_변환한다() throws Exception {
    mockMvc
        .perform(get("/test/validation"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.error.code").value("COMMON-405"));
  }

  @Test
  void 지원하지_않는_미디어_타입을_공통_415_응답으로_변환한다() throws Exception {
    mockMvc
        .perform(post("/test/validation").contentType(MediaType.TEXT_PLAIN).content("name"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.error.code").value("COMMON-415"));
  }

  @Test
  void 예상하지_못한_예외를_공통_500_응답으로_변환하고_내부_메시지는_노출하지_않는다() throws Exception {
    mockMvc
        .perform(get("/test/unexpected"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.error.code").value("COMMON-500"))
        .andExpect(jsonPath("$.error.message").value("서버 내부 오류가 발생했습니다."))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("database password"))));
  }

  @RestController
  @RequestMapping("/test")
  static class TestController {

    @GetMapping("/business")
    void business() {
      throw new BusinessException(TestErrorCode.CONFLICT);
    }

    @PostMapping(value = "/validation", consumes = MediaType.APPLICATION_JSON_VALUE)
    void validation(@Valid @RequestBody TestRequest request) {}

    @GetMapping("/parameter")
    void parameter(@RequestParam String value) {}

    @GetMapping("/parameter-validation")
    void parameterValidation(
        @RequestParam(name = "age") @Min(value = 1, message = "나이는 1 이상이어야 합니다.") Integer age) {}

    @GetMapping("/model-validation")
    void modelValidation(@Valid @ModelAttribute TestModel model) {}

    @GetMapping("/model-binding")
    void modelBinding(@ModelAttribute TestBindingModel model) {}

    @GetMapping("/unexpected")
    void unexpected() {
      throw new IllegalStateException("database password");
    }
  }

  private record TestRequest(@Size(max = 3, message = "이름은 세 글자 이하여야 합니다.") String name) {}

  private static class TestModel {

    @Size(max = 3, message = "이름은 세 글자 이하여야 합니다.")
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  private static class TestBindingModel {

    private Integer count;

    public Integer getCount() {
      return count;
    }

    public void setCount(Integer count) {
      this.count = count;
    }
  }

  private enum TestErrorCode implements ErrorCode {
    CONFLICT;

    @Override
    public HttpStatus status() {
      return HttpStatus.CONFLICT;
    }

    @Override
    public String code() {
      return "TEST-409";
    }

    @Override
    public String message() {
      return "테스트 충돌이 발생했습니다.";
    }
  }
}
