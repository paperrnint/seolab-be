package com.example.seolab.repository;

import com.example.seolab.entity.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {

	Optional<UserBook> findByUserUserIdAndBookBookId(Long userId, Long bookId);

	boolean existsByUserUserIdAndBookBookId(Long userId, Long bookId);

	List<UserBook> findByUserUserIdOrderByCreatedAtDesc(Long userId);

	List<UserBook> findByUserUserIdAndIsReadingOrderByCreatedAtDesc(Long userId, Boolean isReading);

	List<UserBook> findByUserUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(Long userId);

	List<UserBook> findByUserUserIdAndIsFavoriteTrueAndIsReadingOrderByCreatedAtDesc(Long userId, Boolean isReading);

	long countByUserUserIdAndIsReading(Long userId, Boolean isReading);

	Optional<UserBook> findTopByUserUserIdOrderByCreatedAtDesc(Long userId);

	@Query("SELECT ub FROM UserBook ub " +
		"WHERE ub.user.userId = :userId " +
		"AND ub.isReading = :isReading " +
		"ORDER BY ub.updatedAt DESC")
	List<UserBook> findRecentBooks(@Param("userId") Long userId,
		@Param("isReading") Boolean isReading);
}
