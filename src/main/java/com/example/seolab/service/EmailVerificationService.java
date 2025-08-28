package com.example.seolab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final EmailService emailService;

	@Value("${email.verification.expiration}")
	private long expirationSeconds;

	@Value("${email.verification.code-length}")
	private int codeLength;

	private static final String VERIFICATION_PREFIX = "email_verification:";
	private static final String VERIFIED_PREFIX = "email_verified:";

	public void sendVerificationCode(String email) {
		// 이미 인증된 이메일인지 확인
		if (isEmailVerified(email)) {
			throw new IllegalArgumentException("이미 인증된 이메일입니다.");
		}

		// 인증 코드 생성
		String verificationCode = generateVerificationCode();

		// Redis에 저장 (TTL 설정)
		String key = VERIFICATION_PREFIX + email;
		redisTemplate.opsForValue().set(key, verificationCode, Duration.ofSeconds(expirationSeconds));

		// 이메일 발송
		emailService.sendVerificationEmail(email, verificationCode);

		log.info("Verification code sent to email: {}", email);
	}

	public boolean verifyCode(String email, String code) {
		String key = VERIFICATION_PREFIX + email;
		String storedCode = (String) redisTemplate.opsForValue().get(key);

		if (storedCode == null) {
			log.warn("Verification code not found or expired for email: {}", email);
			return false;
		}

		if (!storedCode.equals(code)) {
			log.warn("Invalid verification code for email: {}", email);
			return false;
		}

		// 인증 성공 시 해당 키 삭제하고 인증 완료 상태 저장
		redisTemplate.delete(key);
		markEmailAsVerified(email);

		log.info("Email verification successful for: {}", email);
		return true;
	}

	public boolean isEmailVerified(String email) {
		String verifiedKey = VERIFIED_PREFIX + email;
		return Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey));
	}

	private void markEmailAsVerified(String email) {
		String verifiedKey = VERIFIED_PREFIX + email;
		// 10분간 인증 완료 상태 유지 (회원가입 완료까지의 시간)
		redisTemplate.opsForValue().set(verifiedKey, "verified", Duration.ofMinutes(10));
	}

	public void clearVerifiedStatus(String email) {
		String verifiedKey = VERIFIED_PREFIX + email;
		redisTemplate.delete(verifiedKey);
	}

	private String generateVerificationCode() {
		SecureRandom random = new SecureRandom();
		StringBuilder code = new StringBuilder();

		for (int i = 0; i < codeLength; i++) {
			code.append(random.nextInt(10));
		}

		return code.toString();
	}

	public long getExpirationSeconds() {
		return expirationSeconds;
	}
}
