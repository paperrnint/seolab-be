package com.example.seolab.service;

import com.example.seolab.dto.request.LoginRequest;
import com.example.seolab.dto.request.SignUpRequest;
import com.example.seolab.dto.response.EmailVerificationResponse;
import com.example.seolab.dto.response.LoginResponse;
import com.example.seolab.dto.response.SignUpResponse;
import com.example.seolab.dto.response.TokenResponse;
import com.example.seolab.dto.response.UserInfoResponse;
import com.example.seolab.entity.User;
import com.example.seolab.repository.UserRepository;
import com.example.seolab.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final AuthenticationManager authenticationManager;
	private final EmailVerificationService emailVerificationService;

	public LoginResponse login(LoginRequest loginRequest, HttpServletResponse response) {
		// 인증 시도
		authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(
				loginRequest.getEmail(),
				loginRequest.getPassword()
			)
		);

		// 인증 성공 시 사용자 정보 조회
		User user = userRepository.findByEmail(loginRequest.getEmail())
			.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

		// JWT 토큰 생성
		String accessToken = jwtUtil.generateAccessToken(user);
		String refreshToken = jwtUtil.generateRefreshToken(user);

		// Refresh Token을 HTTP Only 쿠키로 설정
		Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
		refreshTokenCookie.setHttpOnly(true);
		refreshTokenCookie.setSecure(true); // HTTPS에서만 전송
		refreshTokenCookie.setPath("/");
		refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
		response.addCookie(refreshTokenCookie);

		log.info("User {} logged in successfully", user.getEmail());

		return LoginResponse.builder()
			.accessToken(accessToken)
			.email(user.getEmail())
			.username(user.getDisplayName()) // 실제 username 필드 반환
			.build();
	}

	public TokenResponse refreshToken(String refreshToken) {
		// Refresh token 유효성 검증
		if (!jwtUtil.validateToken(refreshToken)) {
			throw new RuntimeException("유효하지 않은 refresh token입니다.");
		}

		// 토큰에서 이메일 추출
		String email = jwtUtil.extractUsername(refreshToken);

		// 사용자 조회
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

		// 새로운 Access Token 생성
		String newAccessToken = jwtUtil.generateAccessToken(user);

		log.info("Access token refreshed for user {}", email);

		return TokenResponse.builder()
			.accessToken(newAccessToken)
			.build();
	}

	public void logout(HttpServletResponse response, Authentication authentication) {
		String email = authentication.getName(); // 로그아웃하는 사용자 식별

		log.info("User {} is logging out", email);

		// Refresh Token 쿠키 삭제
		Cookie refreshTokenCookie = new Cookie("refreshToken", null);
		refreshTokenCookie.setHttpOnly(true);
		refreshTokenCookie.setSecure(true);
		refreshTokenCookie.setPath("/");
		refreshTokenCookie.setMaxAge(0); // 쿠키 만료시간을 0으로 설정하여 삭제
		response.addCookie(refreshTokenCookie);

		log.info("User {} logged out successfully", email);
	}

	// 이메일 인증 코드 발송
	public EmailVerificationResponse sendVerificationCode(String email) {
		// 이미 가입된 이메일인지 확인
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("이미 가입된 이메일입니다.");
		}

		emailVerificationService.sendVerificationCode(email);

		return EmailVerificationResponse.builder()
			.message("인증 코드가 이메일로 발송되었습니다.")
			.expiresInSeconds(emailVerificationService.getExpirationSeconds())
			.build();
	}

	// 인증 코드 검증
	public boolean verifyCode(String email, String code) {
		return emailVerificationService.verifyCode(email, code);
	}

	// 기존 signUp 메서드를 이메일 인증 포함 버전으로 완전 교체
	public SignUpResponse signUp(SignUpRequest signUpRequest) {
		String email = signUpRequest.getEmail();

		// 이메일 인증 여부 확인
		if (!emailVerificationService.isEmailVerified(email)) {
			throw new IllegalArgumentException("이메일 인증이 필요합니다.");
		}

		// 이메일 중복 체크 (다시 한번 확인)
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
		}

		// 이메일에서 username 추출 (@ 앞부분)
		String username = email.split("@")[0];

		// 사용자 생성
		User user = User.builder()
			.email(email)
			.username(username)
			.passwordHash(passwordEncoder.encode(signUpRequest.getPassword()))
			.build();

		User savedUser = userRepository.save(user);

		// 인증 완료 상태 정리
		emailVerificationService.clearVerifiedStatus(email);

		log.info("New user registered with email verification: {}", savedUser.getEmail());

		return SignUpResponse.builder()
			.email(savedUser.getEmail())
			.username(savedUser.getDisplayName())
			.build();
	}

	public UserInfoResponse getCurrentUser(Authentication authentication) {
		String email = authentication.getName();
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

		return UserInfoResponse.builder()
			.email(user.getEmail())
			.username(user.getDisplayName()) // 실제 username 필드 반환
			.build();
	}
}
