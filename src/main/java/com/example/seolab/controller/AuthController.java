package com.example.seolab.controller;

import com.example.seolab.dto.request.LoginRequest;
import com.example.seolab.dto.request.SignUpRequest;
import com.example.seolab.dto.response.LoginResponse;
import com.example.seolab.dto.response.SignUpResponse;
import com.example.seolab.dto.response.TokenResponse;
import com.example.seolab.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<Void> logout(HttpServletResponse response) {
		authService.logout(response);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/signup")
	public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
		SignUpResponse signUpResponse = authService.signUp(signUpRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(signUpResponse);
	}
}
