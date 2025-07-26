package com.example.seolab.repository;

import com.example.seolab.entity.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, UUID> {

	Optional<UserBook> findByUserUserIdAndBookBookId(Long userId, Long bookId);

	boolean existsByUserUserIdAndBookBookId(Long userId, Long bookId);

	// updatedAt 기준으로 변경
	List<UserBook> findByUserUserIdOrderByUpdatedAtDesc(Long userId);

	List<UserBook> findByUserUserIdAndIsReadingOrderByUpdatedAtDesc(Long userId, Boolean isReading);

	List<UserBook> findByUserUserIdAndIsFavoriteTrueOrderByUpdatedAtDesc(Long userId);

	List<UserBook> findByUserUserIdAndIsFavoriteTrueAndIsReadingOrderByUpdatedAtDesc(
		Long userId, Boolean isReading);

	long countByUserUserIdAndIsReading(Long userId, Boolean isReading);

	Optional<UserBook> findTopByUserUserIdOrderByCreatedAtDesc(Long userId);

	@Query("SELECT ub FROM UserBook ub " +
		"WHERE ub.user.userId = :userId " +
		"AND ub.isReading = :isReading " +
		"ORDER BY ub.updatedAt DESC")
	List<UserBook> findRecentBooks(@Param("userId") Long userId,
		@Param("isReading") Boolean isReading);
}
