package com.example.seolab.repository;

import com.example.seolab.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

	// 단순 필드 검색은 기존 JPA 메소드명 방식 유지
	Optional<Book> findByIsbn(String isbn);
	boolean existsByIsbn(String isbn);

	// JSON 배열 검색이 필요한 경우만 native query 사용
	@Query(value = "SELECT * FROM books WHERE " +
		"title = :title AND " +
		"JSON_CONTAINS(authors, JSON_QUOTE(:author)) = 1 AND " +
		"publisher = :publisher", nativeQuery = true)
	Optional<Book> findByTitleAndAuthorAndPublisher(
		@Param("title") String title,
		@Param("author") String author,
		@Param("publisher") String publisher
	);

	@Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM books WHERE " +
		"title = :title AND " +
		"JSON_CONTAINS(authors, JSON_QUOTE(:author)) = 1 AND " +
		"publisher = :publisher", nativeQuery = true)
	boolean existsByTitleAndAuthorAndPublisher(
		@Param("title") String title,
		@Param("author") String author,
		@Param("publisher") String publisher
	);

	@Query(value = "SELECT * FROM books WHERE " +
		"(isbn = :isbn AND :isbn IS NOT NULL) OR " +
		"(title = :title AND JSON_CONTAINS(authors, JSON_QUOTE(:author)) = 1 AND publisher = :publisher)",
		nativeQuery = true)
	Optional<Book> findByIsbnOrTitleAndAuthorAndPublisher(
		@Param("isbn") String isbn,
		@Param("title") String title,
		@Param("author") String author,
		@Param("publisher") String publisher
	);
}
