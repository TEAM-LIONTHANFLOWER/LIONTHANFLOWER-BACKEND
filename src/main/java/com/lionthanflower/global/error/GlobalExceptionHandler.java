// 애플리케이션 예외를 표준 API 오류 응답으로 변환하는 전역 처리기
package com.lionthanflower.global.error;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
    return createResponse(exception.errorCode());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    return createValidationResponse(exception.getBindingResult().getFieldErrors());
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBindException(BindException exception) {
    return createValidationResponse(exception.getBindingResult().getFieldErrors());
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ErrorResponse> handleHandlerMethodValidationException(
      HandlerMethodValidationException exception) {
    if (exception.isForReturnValue()) {
      log.error("Return value validation failed", exception);
      return createResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    List<ErrorResponse.FieldError> fieldErrors =
        exception.getParameterValidationResults().stream()
            .flatMap(result -> validationErrors(result).stream())
            .toList();

    return createValidationErrorResponse(fieldErrors);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorResponse> handleBadRequestException() {
    return createResponse(CommonErrorCode.INVALID_INPUT_VALUE);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFoundException() {
    return createResponse(CommonErrorCode.NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException exception) {
    return createResponse(CommonErrorCode.METHOD_NOT_ALLOWED, exception.getHeaders());
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException() {
    return createResponse(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception exception) {
    log.error("Unhandled exception", exception);
    return createResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ErrorResponse> createResponse(ErrorCode errorCode) {
    return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode));
  }

  private ResponseEntity<ErrorResponse> createResponse(ErrorCode errorCode, HttpHeaders headers) {
    return ResponseEntity.status(errorCode.status())
        .headers(headers)
        .body(ErrorResponse.of(errorCode));
  }

  private ResponseEntity<ErrorResponse> createValidationResponse(List<FieldError> fieldErrors) {
    List<ErrorResponse.FieldError> responseFieldErrors =
        fieldErrors.stream()
            .map(
                fieldError ->
                    new ErrorResponse.FieldError(fieldError.getField(), message(fieldError)))
            .toList();
    return createValidationErrorResponse(responseFieldErrors);
  }

  private ResponseEntity<ErrorResponse> createValidationErrorResponse(
      List<ErrorResponse.FieldError> fieldErrors) {
    ErrorCode errorCode = CommonErrorCode.INVALID_INPUT_VALUE;
    return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode, fieldErrors));
  }

  private String parameterName(ParameterValidationResult result) {
    MethodParameter parameter = result.getMethodParameter();
    String parameterName = parameter.getParameterName();
    if (parameterName != null) {
      return parameterName;
    }

    RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
    if (requestParam != null && !requestParam.name().isBlank()) {
      return requestParam.name();
    }
    if (requestParam != null && !requestParam.value().isBlank()) {
      return requestParam.value();
    }
    return "parameter";
  }

  private List<ErrorResponse.FieldError> validationErrors(ParameterValidationResult result) {
    if (result instanceof ParameterErrors parameterErrors) {
      return parameterErrors.getFieldErrors().stream()
          .map(
              fieldError ->
                  new ErrorResponse.FieldError(fieldError.getField(), message(fieldError)))
          .toList();
    }

    return result.getResolvableErrors().stream()
        .map(error -> new ErrorResponse.FieldError(parameterName(result), message(error)))
        .toList();
  }

  private String message(MessageSourceResolvable error) {
    return error.getDefaultMessage() == null
        ? CommonErrorCode.INVALID_INPUT_VALUE.message()
        : error.getDefaultMessage();
  }

  private String message(FieldError error) {
    return error.isBindingFailure()
        ? CommonErrorCode.INVALID_INPUT_VALUE.message()
        : message((MessageSourceResolvable) error);
  }
}
