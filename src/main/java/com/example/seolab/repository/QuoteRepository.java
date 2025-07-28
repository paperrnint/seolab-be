package com.example.seolab.repository;

import com.example.seolab.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, UUID> {

	// 특정 사용자 책의 모든 문장 조회 (최신순)
	List<Quote> findByUserBookUserBookIdOrderByCreatedAtAsc(UUID userBookId);

	// 특정 사용자 책의 즐겨찾기 문장만 조회
	List<Quote> findByUserBookUserBookIdAndIsFavoriteTrueOrderByCreatedAtAsc(UUID userBookId);

	// 특정 사용자의 모든 문장 조회 (최신순)
	@Query("SELECT q FROM Quote q " +
		"WHERE q.userBook.user.userId = :userId " +
		"ORDER BY q.createdAt DESC")
	List<Quote> findByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId);

	// 특정 사용자의 즐겨찾기 문장만 조회
	@Query("SELECT q FROM Quote q " +
		"WHERE q.userBook.user.userId = :userId " +
		"AND q.isFavorite = true " +
		"ORDER BY q.createdAt DESC")
	List<Quote> findFavoriteQuotesByUserId(@Param("userId") Long userId);

	// 특정 사용자 책의 문장 개수
	long countByUserBookUserBookId(UUID userBookId);

	// 특정 사용자의 최근 문장들 (홈 화면용)
	@Query("SELECT q FROM Quote q " +
		"WHERE q.userBook.user.userId = :userId " +
		"ORDER BY q.createdAt DESC")
	List<Quote> findRecentQuotesByUserId(@Param("userId") Long userId);

	// 사용자와 문장 ID로 조회 (권한 체크용)
	@Query("SELECT q FROM Quote q " +
		"WHERE q.quoteId = :quoteId " +
		"AND q.userBook.user.userId = :userId")
	Optional<Quote> findByQuoteIdAndUserId(@Param("quoteId") UUID quoteId,
		@Param("userId") Long userId);
}
