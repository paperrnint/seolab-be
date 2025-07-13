package com.example.seolab.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationExceptions(
		MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		return ResponseEntity.badRequest().body(errors);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleBadCredentialsException(
		BadCredentialsException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", "이메일 또는 비밀번호가 일치하지 않습니다.");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleUsernameNotFoundException(
		UsernameNotFoundException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", "사용자를 찾을 수 없습니다.");
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgumentException(
		IllegalArgumentException ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", ex.getMessage());
		return ResponseEntity.badRequest().body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleGlobalException(
		Exception ex) {
		log.error("Unexpected error occurred", ex);
		Map<String, String> error = new HashMap<>();
		error.put("message", "서버 오류가 발생했습니다.");
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	@ExceptionHandler({JwtException.class, io.jsonwebtoken.security.SecurityException.class})
	public ResponseEntity<Map<String, String>> handleJwtException(Exception ex) {
		Map<String, String> error = new HashMap<>();
		error.put("message", "유효하지 않은 토큰입니다.");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}
}
