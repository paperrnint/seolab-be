package com.example.seolab.unit.service;

import com.example.seolab.dto.request.LoginRequest;
import com.example.seolab.dto.request.SignUpRequest;
import com.example.seolab.dto.response.EmailVerificationResponse;
import com.example.seolab.dto.response.LoginResponse;
import com.example.seolab.dto.response.SignUpResponse;
import com.example.seolab.dto.response.TokenResponse;
import com.example.seolab.entity.User;
import com.example.seolab.repository.UserRepository;
import com.example.seolab.security.JwtUtil;
import com.example.seolab.service.AuthService;
import com.example.seolab.service.EmailVerificationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtUtil jwtUtil;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private EmailVerificationService emailVerificationService;

	@Mock
	private HttpServletResponse response;

	@InjectMocks
	private AuthService authService;

	private User testUser;
	private LoginRequest loginRequest;

	@BeforeEach
	void setUp() {
		testUser = User.builder()
			.userId(1L)
			.email("test@example.com")
			.username("test")
			.passwordHash("encodedPassword")
			.build();

		loginRequest = new LoginRequest("test@example.com", "password123!");
	}

	@Test
	@DisplayName("유효한 이메일과 비밀번호로 로그인하면 AccessToken과 사용자 정보를 반환")
	void login_withValidCredentials_returnsTokenAndUserInfo() {
		// given
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(null);
		when(userRepository.findByEmail("test@example.com"))
			.thenReturn(Optional.of(testUser));
		when(jwtUtil.generateAccessToken(testUser))
			.thenReturn("access-token");
		when(jwtUtil.generateRefreshToken(testUser))
			.thenReturn("refresh-token");

		// when
		LoginResponse result = authService.login(loginRequest, response);

		// then
		assertThat(result.getAccessToken()).isEqualTo("access-token");
		assertThat(result.getEmail()).isEqualTo("test@example.com");
		assertThat(result.getUsername()).isEqualTo("test");

		// RefreshToken이 쿠키로 설정되었는지 확인
		ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
		verify(response).addCookie(cookieCaptor.capture());

		Cookie cookie = cookieCaptor.getValue();
		assertThat(cookie.getName()).isEqualTo("refreshToken");
		assertThat(cookie.getValue()).isEqualTo("refresh-token");
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.getSecure()).isTrue();
		assertThat(cookie.getMaxAge()).isEqualTo(7 * 24 * 60 * 60);
	}

	@Test
	@DisplayName("잘못된 비밀번호로 로그인하면 예외가 발생")
	void login_withInvalidPassword_throwsException() {
		// given
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenThrow(new BadCredentialsException("Bad credentials"));

		// when & then
		assertThatThrownBy(() -> authService.login(loginRequest, response))
			.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	@DisplayName("존재하지 않는 사용자로 로그인하면 예외가 발생")
	void login_withNonExistentUser_throwsException() {
		// given
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
			.thenReturn(null);
		when(userRepository.findByEmail(anyString()))
			.thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> authService.login(loginRequest, response))
			.isInstanceOf(UsernameNotFoundException.class)
			.hasMessageContaining("사용자를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("유효한 RefreshToken으로 AccessToken을 갱신가능")
	void refreshToken_withValidToken_returnsNewAccessToken() {
		// given
		String refreshToken = "valid-refresh-token";
		when(jwtUtil.validateToken(refreshToken)).thenReturn(true);
		when(jwtUtil.extractUsername(refreshToken)).thenReturn("test@example.com");
		when(userRepository.findByEmail("test@example.com"))
			.thenReturn(Optional.of(testUser));
		when(jwtUtil.generateAccessToken(testUser))
			.thenReturn("new-access-token");

		// when
		TokenResponse result = authService.refreshToken(refreshToken);

		// then
		assertThat(result.getAccessToken()).isEqualTo("new-access-token");
	}

	@Test
	@DisplayName("유효하지 않은 RefreshToken으로 갱신하면 예외가 발생")
	void refreshToken_withInvalidToken_throwsException() {
		// given
		String invalidToken = "invalid-token";
		when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> authService.refreshToken(invalidToken))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("유효하지 않은 refresh token");
	}

	@Test
	@DisplayName("이메일 인증 코드 발송 요청 시 이미 가입된 이메일이면 예외가 발생")
	void sendVerificationCode_withExistingEmail_throwsException() {
		// given
		String email = "existing@example.com";
		when(userRepository.existsByEmail(email)).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> authService.sendVerificationCode(email))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("이미 가입된 이메일");
	}

	@Test
	@DisplayName("신규 이메일로 인증 코드 발송 요청 시 성공")
	void sendVerificationCode_withNewEmail_succeeds() {
		// given
		String email = "new@example.com";
		when(userRepository.existsByEmail(email)).thenReturn(false);
		doNothing().when(emailVerificationService).sendVerificationCode(email);
		when(emailVerificationService.getExpirationSeconds()).thenReturn(300L);

		// when
		EmailVerificationResponse result = authService.sendVerificationCode(email);

		// then
		assertThat(result.getMessage()).contains("인증 코드가 이메일로 발송");
		assertThat(result.getExpiresInSeconds()).isEqualTo(300L);
		verify(emailVerificationService).sendVerificationCode(email);
	}

	@Test
	@DisplayName("이메일 인증 완료 후 회원가입하면 사용자가 생성")
	void signUp_withVerifiedEmail_createsUser() {
		// given
		SignUpRequest request = new SignUpRequest(
			"new@example.com",
			"Password123!"
		);

		when(emailVerificationService.isEmailVerified(request.getEmail()))
			.thenReturn(true);
		when(userRepository.existsByEmail(request.getEmail()))
			.thenReturn(false);
		when(passwordEncoder.encode(request.getPassword()))
			.thenReturn("encodedPassword");
		when(userRepository.save(any(User.class)))
			.thenReturn(testUser);

		// when
		SignUpResponse result = authService.signUp(request);

		// then
		assertThat(result.getEmail()).isEqualTo("test@example.com");
		assertThat(result.getUsername()).isEqualTo("test");

		verify(userRepository).save(any(User.class));
		verify(emailVerificationService).clearVerifiedStatus(request.getEmail());
	}

	@Test
	@DisplayName("이메일 인증 없이 회원가입하면 예외가 발생한다")
	void signUp_withoutEmailVerification_throwsException() {
		// given
		SignUpRequest request = new SignUpRequest(
			"unverified@example.com",
			"Password123!"
		);

		when(emailVerificationService.isEmailVerified(request.getEmail()))
			.thenReturn(false);

		// when & then
		assertThatThrownBy(() -> authService.signUp(request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("이메일 인증이 필요");
	}

	@Test
	@DisplayName("이미 가입된 이메일로 회원가입하면 예외가 발생")
	void signUp_withDuplicateEmail_throwsException() {
		// given
		SignUpRequest request = new SignUpRequest(
			"existing@example.com",
			"Password123!"
		);

		when(emailVerificationService.isEmailVerified(request.getEmail()))
			.thenReturn(true);
		when(userRepository.existsByEmail(request.getEmail()))
			.thenReturn(true);

		// when & then
		assertThatThrownBy(() -> authService.signUp(request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("이미 사용중인 이메일");
	}
}
