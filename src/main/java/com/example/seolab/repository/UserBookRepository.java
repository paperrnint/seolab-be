package com.example.seolab.repository;

import com.example.seolab.entity.UserBook;
import com.example.seolab.entity.UserBook.ReadingStatus;
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

	List<UserBook> findByUserUserIdAndReadingStatusOrderByCreatedAtDesc(Long userId, ReadingStatus readingStatus);

	List<UserBook> findByUserUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(Long userId);

	List<UserBook> findByUserUserIdAndIsFavoriteTrueAndReadingStatusOrderByCreatedAtDesc(Long userId, ReadingStatus readingStatus);

	long countByUserUserIdAndReadingStatus(Long userId, ReadingStatus readingStatus);

	Optional<UserBook> findTopByUserUserIdOrderByCreatedAtDesc(Long userId);

	@Query("SELECT ub FROM UserBook ub " +
		"WHERE ub.user.userId = :userId " +
		"AND ub.readingStatus = :status " +
		"ORDER BY ub.updatedAt DESC")
	List<UserBook> findRecentReadingBooks(@Param("userId") Long userId,
		@Param("status") ReadingStatus status);
}
