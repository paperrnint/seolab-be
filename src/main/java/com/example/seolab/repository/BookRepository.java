package com.example.seolab.repository;

import com.example.seolab.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

	/**
	 * ISBN으로 책 찾기
	 * @param isbn ISBN 번호
	 * @return 찾은 책 (Optional)
	 */
	Optional<Book> findByIsbn(String isbn);

	/**
	 * 제목, 저자, 출판사 조합으로 책 찾기
	 * @param title 책 제목
	 * @param author 저자
	 * @param publisher 출판사
	 * @return 찾은 책 (Optional)
	 */
	Optional<Book> findByTitleAndAuthorAndPublisher(String title, String author, String publisher);

	/**
	 * ISBN이 존재하는지 확인
	 * @param isbn ISBN 번호
	 * @return 존재 여부
	 */
	boolean existsByIsbn(String isbn);

	/**
	 * 제목, 저자, 출판사 조합이 존재하는지 확인
	 * @param title 책 제목
	 * @param author 저자
	 * @param publisher 출판사
	 * @return 존재 여부
	 */
	boolean existsByTitleAndAuthorAndPublisher(String title, String author, String publisher);

	/**
	 * ISBN 또는 제목+저자+출판사로 책 찾기 (복합 검색)
	 * @param isbn ISBN 번호
	 * @param title 책 제목
	 * @param author 저자
	 * @param publisher 출판사
	 * @return 찾은 책 (Optional)
	 */
	@Query("SELECT b FROM Book b WHERE " +
		"(b.isbn = :isbn AND :isbn IS NOT NULL) OR " +
		"(b.title = :title AND b.author = :author AND b.publisher = :publisher)")
	Optional<Book> findByIsbnOrTitleAndAuthorAndPublisher(
		@Param("isbn") String isbn,
		@Param("title") String title,
		@Param("author") String author,
		@Param("publisher") String publisher
	);
}
