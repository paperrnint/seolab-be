package com.example.seolab.integration.api;

import com.example.seolab.dto.request.LoginRequest;
import com.example.seolab.dto.request.SignUpRequest;
import com.example.seolab.entity.User;
import com.example.seolab.repository.UserRepository;
import com.example.seolab.service.EmailVerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Auth API 통합 테스트")
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private EmailVerificationService emailVerificationService;

	private User testUser;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();

		testUser = User.builder()
			.email("test@example.com")
			.username("test")
			.passwordHash(passwordEncoder.encode("Password123!"))
			.build();
		userRepository.save(testUser);
	}

	@Test
	@DisplayName("POST /api/auth/login - 유효한 인증 정보로 로그인에 성공")
	void login_withValidCredentials_returnsAccessToken() throws Exception {
		// given
		LoginRequest request = new LoginRequest("test@example.com", "Password123!");

		// when & then
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists())
			.andExpect(jsonPath("$.email").value("test@example.com"))
			.andExpect(jsonPath("$.username").value("test"))
			.andExpect(cookie().exists("refreshToken"))
			.andExpect(cookie().httpOnly("refreshToken", true))
			.andExpect(cookie().secure("refreshToken", true));
	}

	@Test
	@DisplayName("POST /api/auth/login - 잘못된 비밀번호로 로그인에 실패")
	void login_withInvalidPassword_returns401() throws Exception {
		// given
		LoginRequest request = new LoginRequest("test@example.com", "WrongPassword!");

		// when & then
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value(containsString("이메일 또는 비밀번호가 일치하지 않습니다")));
	}

	@Test
	@DisplayName("POST /api/auth/login - 존재하지 않는 이메일로 로그인에 실패")
	void login_withNonExistentEmail_returns401() throws Exception {
		// given
		LoginRequest request = new LoginRequest("nonexistent@example.com", "Password123!");

		// when & then
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("POST /api/auth/login - 이메일 형식이 잘못되면 400을 반환")
	void login_withInvalidEmailFormat_returns400() throws Exception {
		// given
		LoginRequest request = new LoginRequest("invalid-email", "Password123!");

		// when & then
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(containsString("이메일")));
	}

	@Test
	@DisplayName("POST /api/auth/signup - 이메일 인증 후 회원가입에 성공")
	void signup_withVerifiedEmail_returnsCreated() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest("new@example.com", "Password123!");
		when(emailVerificationService.isEmailVerified("new@example.com")).thenReturn(true);

		// when & then
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.email").value("new@example.com"))
			.andExpect(jsonPath("$.username").exists());
	}

	@Test
	@DisplayName("POST /api/auth/signup - 이메일 인증 없이 회원가입하면 400을 반환")
	void signup_withoutEmailVerification_returns400() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest("unverified@example.com", "Password123!");
		when(emailVerificationService.isEmailVerified(anyString())).thenReturn(false);

		// when & then
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(containsString("이메일 인증이 필요")));
	}

	@Test
	@DisplayName("POST /api/auth/signup - 이미 가입된 이메일로 회원가입하면 409를 반환")
	void signup_withExistingEmail_returns409() throws Exception {
		// given
		SignUpRequest request = new SignUpRequest("test@example.com", "Password123!");
		when(emailVerificationService.isEmailVerified("test@example.com")).thenReturn(true);

		// when & then
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value(containsString("이미 사용중인 이메일")));
	}

	@Test
	@DisplayName("POST /api/auth/signup - 비밀번호가 규칙에 맞지 않으면 400을 반환")
	void signup_withInvalidPassword_returns400() throws Exception {
		// given - 비밀번호가 너무 짧음
		SignUpRequest request = new SignUpRequest("new@example.com", "Pass1!");

		// when & then
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(containsString("비밀번호")));
	}

	@Test
	@DisplayName("POST /api/auth/verify/request - 신규 이메일로 인증 코드 발송 요청에 성공")
	void sendVerificationCode_withNewEmail_returnsOk() throws Exception {
		// given
		String newEmail = "new@example.com";
		when(emailVerificationService.getExpirationSeconds()).thenReturn(300L);

		// when & then
		mockMvc.perform(post("/api/auth/verify/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + newEmail + "\"}"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(containsString("인증 코드가 이메일로 발송")))
			.andExpect(jsonPath("$.expiresInSeconds").value(300));
	}

	@Test
	@DisplayName("POST /api/auth/verify/request - 이미 가입된 이메일로 요청하면 409를 반환")
	void sendVerificationCode_withExistingEmail_returns409() throws Exception {
		// when & then
		mockMvc.perform(post("/api/auth/verify/request")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"test@example.com\"}"))
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value(containsString("이미 가입된 이메일")));
	}

	@Test
	@DisplayName("POST /api/auth/verify - 올바른 인증 코드로 검증에 성공")
	void verifyCode_withValidCode_returnsOk() throws Exception {
		// given
		when(emailVerificationService.verifyCode("test@example.com", "123456")).thenReturn(true);

		// when & then
		mockMvc.perform(post("/api/auth/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"test@example.com\",\"code\":\"123456\"}"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("이메일 인증이 완료되었습니다."));
	}

	@Test
	@DisplayName("POST /api/auth/verify - 잘못된 인증 코드로 검증에 실패")
	void verifyCode_withInvalidCode_returns422() throws Exception {
		// given
		when(emailVerificationService.verifyCode("test@example.com", "999999")).thenReturn(false);

		// when & then
		mockMvc.perform(post("/api/auth/verify")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"test@example.com\",\"code\":\"999999\"}"))
			.andDo(print())
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.message").value(containsString("인증 코드가 올바르지 않거나 만료")));
	}

	@Test
	@DisplayName("POST /api/auth/refresh - 유효한 RefreshToken으로 AccessToken을 갱신 가능")
	void refreshToken_withValidToken_returnsNewAccessToken() throws Exception {
		// given - 먼저 로그인하여 RefreshToken 획득
		LoginRequest loginRequest = new LoginRequest("test@example.com", "Password123!");

		var loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn();

		String refreshToken = loginResult.getResponse().getCookie("refreshToken").getValue();

		// when & then - RefreshToken으로 갱신 요청
		mockMvc.perform(post("/api/auth/refresh")
				.cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshToken)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	@DisplayName("POST /api/auth/refresh - RefreshToken이 없으면 401을 반환")
	void refreshToken_withoutToken_returns401() throws Exception {
		// when & then
		mockMvc.perform(post("/api/auth/refresh"))
			.andDo(print())
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("POST /api/auth/logout - 로그아웃 시 RefreshToken 쿠키가 삭제")
	void logout_deletesRefreshTokenCookie() throws Exception {
		// given - 먼저 로그인
		LoginRequest loginRequest = new LoginRequest("test@example.com", "Password123!");

		var loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn();

		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
			.get("accessToken").asText();

		// when & then - 로그아웃
		mockMvc.perform(post("/api/auth/logout")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(cookie().maxAge("refreshToken", 0));
	}

	@Test
	@DisplayName("GET /api/auth/me - 인증된 사용자의 정보 조회 가능")
	void getCurrentUser_withValidToken_returnsUserInfo() throws Exception {
		// given - 먼저 로그인
		LoginRequest loginRequest = new LoginRequest("test@example.com", "Password123!");

		var loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginRequest)))
			.andReturn();

		String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
			.get("accessToken").asText();

		// when & then
		mockMvc.perform(get("/api/auth/me")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("test@example.com"))
			.andExpect(jsonPath("$.username").value("test"));
	}

	@Test
	@DisplayName("GET /api/auth/me - 인증 없이 요청하면 401을 반환")
	void getCurrentUser_withoutToken_returns401() throws Exception {
		// when & then
		mockMvc.perform(get("/api/auth/me"))
			.andDo(print())
			.andExpect(status().isUnauthorized());
	}
}
