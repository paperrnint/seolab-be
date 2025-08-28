package com.example.seolab.controller;

import java.util.HashMap;
import java.util.Map;

import com.example.seolab.dto.request.EmailVerificationRequest;
import com.example.seolab.dto.request.LoginRequest;
import com.example.seolab.dto.request.SignUpRequest;
import com.example.seolab.dto.request.VerifyCodeRequest;
import com.example.seolab.dto.response.EmailVerificationResponse;
import com.example.seolab.dto.response.LoginResponse;
import com.example.seolab.dto.response.SignUpResponse;
import com.example.seolab.dto.response.TokenResponse;
import com.example.seolab.dto.response.UserInfoResponse;
import com.example.seolab.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
		HttpServletResponse response) {
		LoginResponse loginResponse = authService.login(loginRequest, response);
		return ResponseEntity.ok(loginResponse);
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refreshToken(HttpServletRequest request) {
		// 쿠키에서 Refresh Token 추출
		String refreshToken = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("refreshToken".equals(cookie.getName())) {
					refreshToken = cookie.getValue();
					break;
				}
			}
		}

		if (refreshToken == null) {
			return ResponseEntity.status(401).build();
		}

		TokenResponse tokenResponse = authService.refreshToken(refreshToken);
		return ResponseEntity.ok(tokenResponse);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletResponse response, Authentication authentication) {
		// authentication이 null이면 Spring Security가 자동으로 401 반환
		authService.logout(response, authentication);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/signup")
	public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
		SignUpResponse signUpResponse = authService.signUp(signUpRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(signUpResponse);
	}

	@PostMapping("/verify/request")
	public ResponseEntity<EmailVerificationResponse> sendVerificationCode(
		@Valid @RequestBody EmailVerificationRequest request) {

		EmailVerificationResponse response = authService.sendVerificationCode(request.getEmail());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/verify")
	public ResponseEntity<Map<String, String>> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
		boolean isValid = authService.verifyCode(request.getEmail(), request.getCode());

		Map<String, String> response = new HashMap<>();
		if (isValid) {
			response.put("message", "이메일 인증이 완료되었습니다.");
			return ResponseEntity.ok(response);
		} else {
			response.put("message", "인증 코드가 올바르지 않거나 만료되었습니다.");
			return ResponseEntity.badRequest().body(response);
		}
	}

	@GetMapping("/me")
	public ResponseEntity<UserInfoResponse> getCurrentUser(Authentication authentication) {
		UserInfoResponse userInfo = authService.getCurrentUser(authentication);
		return ResponseEntity.ok(userInfo);
	}
}
