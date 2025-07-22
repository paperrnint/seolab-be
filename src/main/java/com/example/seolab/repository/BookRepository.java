package com.example.seolab.repository;

import com.example.seolab.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {


	Optional<Book> findByIsbn(String isbn);

	Optional<Book> findByTitleAndAuthorAndPublisher(String title, String author, String publisher);

	boolean existsByIsbn(String isbn);

	boolean existsByTitleAndAuthorAndPublisher(String title, String author, String publisher);

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
