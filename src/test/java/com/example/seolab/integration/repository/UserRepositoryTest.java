package com.example.seolab.integration.repository;

import com.example.seolab.entity.User;
import com.example.seolab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository 통합 테스트")
class UserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	private User testUser;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();

		testUser = User.builder()
			.email("test@example.com")
			.username("testuser")
			.passwordHash("encodedPassword123")
			.build();
	}

	@Test
	@DisplayName("사용자를 저장하고 조회할 수 있음")
	void saveAndFindUser() {
		// when
		User savedUser = userRepository.save(testUser);

		// then
		assertThat(savedUser.getUserId()).isNotNull();
		assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
		assertThat(savedUser.getDisplayName()).isEqualTo("testuser");
		assertThat(savedUser.getCreatedAt()).isNotNull();
		assertThat(savedUser.getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("이메일로 사용자를 찾을 수 있음")
	void findByEmail_returnsUser() {
		// given
		userRepository.save(testUser);

		// when
		Optional<User> found = userRepository.findByEmail("test@example.com");

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("test@example.com");
		assertThat(found.get().getDisplayName()).isEqualTo("testuser"); // ← getDisplayName() 사용
	}

	@Test
	@DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환")
	void findByEmail_withNonExistentEmail_returnsEmpty() {
		// when
		Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

		// then
		assertThat(found).isEmpty();
	}

	@Test
	@DisplayName("이메일 존재 여부 확인 가능")
	void existsByEmail_returnsTrue() {
		// given
		userRepository.save(testUser);

		// when
		boolean exists = userRepository.existsByEmail("test@example.com");

		// then
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 이메일은 false를 반환")
	void existsByEmail_withNonExistentEmail_returnsFalse() {
		// when
		boolean exists = userRepository.existsByEmail("nonexistent@example.com");

		// then
		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("이메일은 유니크 제약조건을 가짐")
	void email_hasUniqueConstraint() {
		// given
		userRepository.save(testUser);

		User duplicateUser = User.builder()
			.email("test@example.com")
			.username("another")
			.passwordHash("password")
			.build();

		// when & then
		try {
			userRepository.saveAndFlush(duplicateUser);
			assertThat(false).as("중복 이메일로 저장되면 안됨").isTrue();
		} catch (Exception e) {
			assertThat(e).isNotNull();
		}
	}

	@Test
	@DisplayName("사용자 정보 수정 가능")
	void updateUser() {
		// given
		User savedUser = userRepository.save(testUser);
		Long userId = savedUser.getUserId();

		// when
		// username 필드를 직접 수정
		savedUser.setUsername("updatedName");
		userRepository.save(savedUser);

		// then
		Optional<User> updated = userRepository.findById(userId);
		assertThat(updated).isPresent();
		assertThat(updated.get().getDisplayName()).isEqualTo("updatedName");
		assertThat(updated.get().getUpdatedAt()).isNotNull();
	}

	@Test
	@DisplayName("사용자 삭제 가능")
	void deleteUser() {
		// given
		User savedUser = userRepository.save(testUser);
		Long userId = savedUser.getUserId();

		// when
		userRepository.delete(savedUser);

		// then
		Optional<User> deleted = userRepository.findById(userId);
		assertThat(deleted).isEmpty();
	}
}
