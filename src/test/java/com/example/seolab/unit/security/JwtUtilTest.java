package com.example.seolab.unit.security;

import com.example.seolab.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtUtil 단위 테스트")
class JwtUtilTest {

	private JwtUtil jwtUtil;
	private UserDetails userDetails;

	@BeforeEach
	void setUp() {
		jwtUtil = new JwtUtil();

		// 테스트용 비밀키 설정 (최소 256비트)
		String testSecret = "dGVzdFNlY3JldEtleUZvckpXVFRlc3RpbmdQdXJwb3Nlc09ubHlNdXN0QmVMb25nRW5vdWdo";
		ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
		ReflectionTestUtils.setField(jwtUtil, "accessTokenValidity", 3600000L); // 1시간
		ReflectionTestUtils.setField(jwtUtil, "refreshTokenValidity", 604800000L); // 7일

		userDetails = User.builder()
			.username("test@example.com")
			.password("password")
			.authorities(Collections.emptyList())
			.build();
	}

	@Test
	@DisplayName("AccessToken 생성 시 유효한 JWT 토큰이 생성")
	void generateAccessToken_createsValidToken() {
		// when
		String token = jwtUtil.generateAccessToken(userDetails);

		// then
		assertThat(token).isNotNull();
		assertThat(token).contains(".");
		assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
	}

	@Test
	@DisplayName("RefreshToken 생성 시 유효한 JWT 토큰이 생성")
	void generateRefreshToken_createsValidToken() {
		// when
		String token = jwtUtil.generateRefreshToken(userDetails);

		// then
		assertThat(token).isNotNull();
		assertThat(token).contains(".");
	}

	@Test
	@DisplayName("토큰에서 사용자 이메일을 추출할 수 있다")
	void extractUsername_returnsCorrectEmail() {
		// given
		String token = jwtUtil.generateAccessToken(userDetails);

		// when
		String username = jwtUtil.extractUsername(token);

		// then
		assertThat(username).isEqualTo("test@example.com");
	}

	@Test
	@DisplayName("토큰에서 만료 시간을 추출할 수 있음")
	void extractExpiration_returnsValidDate() {
		// given
		String token = jwtUtil.generateAccessToken(userDetails);

		// when
		Date expiration = jwtUtil.extractExpiration(token);

		// then
		assertThat(expiration).isAfter(new Date());
	}

	@Test
	@DisplayName("유효한 토큰은 검증을 통과")
	void validateToken_withValidToken_returnsTrue() {
		// given
		String token = jwtUtil.generateAccessToken(userDetails);

		// when
		Boolean isValid = jwtUtil.validateToken(token, userDetails);

		// then
		assertThat(isValid).isTrue();
	}

	@Test
	@DisplayName("잘못된 사용자의 토큰은 검증에 실패")
	void validateToken_withWrongUser_returnsFalse() {
		// given
		String token = jwtUtil.generateAccessToken(userDetails);

		UserDetails wrongUser = User.builder()
			.username("wrong@example.com")
			.password("password")
			.authorities(Collections.emptyList())
			.build();

		// when
		Boolean isValid = jwtUtil.validateToken(token, wrongUser);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("잘못된 형식의 토큰은 검증에 실패")
	void validateToken_withInvalidFormat_returnsFalse() {
		// given
		String invalidToken = "invalid.token.format";

		// when
		Boolean isValid = jwtUtil.validateToken(invalidToken);

		// then
		assertThat(isValid).isFalse();
	}

	@Test
	@DisplayName("AccessToken과 RefreshToken의 만료 시간이 다름")
	void accessTokenAndRefreshToken_haveDifferentExpiration() {
		// when
		String accessToken = jwtUtil.generateAccessToken(userDetails);
		String refreshToken = jwtUtil.generateRefreshToken(userDetails);

		Date accessExpiration = jwtUtil.extractExpiration(accessToken);
		Date refreshExpiration = jwtUtil.extractExpiration(refreshToken);

		// then
		assertThat(refreshExpiration).isAfter(accessExpiration);
	}
}
