package com.example.closestv2.api.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class RestControllerExceptionAdvice {
    @ExceptionHandler(IllegalArgumentException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> illegalArgumentException(IllegalArgumentException e) {
        ApiErrorResponse<T> errorResponse = ApiErrorResponse.error(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> bindException(MethodArgumentNotValidException e) {

        ApiErrorResponse<T> errorResponse = ApiErrorResponse.error(
                (HttpStatus) e.getStatusCode(),
                // 첫 번째 에러의 메시지 꺼내기..
                e.getFieldErrors().getFirst().getDefaultMessage()
        );

        for (FieldError fieldError : e.getFieldErrors()) {
            errorResponse.addValidation(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public <T> ResponseEntity<ApiErrorResponse<T>> constraintViolationException(ConstraintViolationException e) {

        ApiErrorResponse<T> errorResponse = ApiErrorResponse.error(
                // 첫 번째 에러의 메시지 꺼내기..
                HttpStatus.BAD_REQUEST,
                e.getConstraintViolations().stream().toList().getFirst().getMessage()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }
}
