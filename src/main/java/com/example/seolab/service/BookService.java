package com.example.seolab.service;

import com.example.seolab.dto.response.BookDto;
import com.example.seolab.entity.Book;
import com.example.seolab.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookService {

	private final BookRepository bookRepository;

	public Book findOrCreateBook(BookDto bookDto) {
		log.info("Finding or creating book: {}", bookDto.getTitle());

		// 1. ISBN으로 먼저 찾기 (있는 경우)
		String firstIsbn = extractFirstIsbn(bookDto.getIsbn());
		if (StringUtils.hasText(firstIsbn)) {
			Optional<Book> existingByIsbn = bookRepository.findByIsbn(firstIsbn);
			if (existingByIsbn.isPresent()) {
				log.info("Found existing book by ISBN: {}", firstIsbn);
				return existingByIsbn.get();
			}
		}

		// 2. title + author + publisher 조합으로 찾기
		String firstAuthor = getFirstAuthor(bookDto);
		if (StringUtils.hasText(bookDto.getTitle()) &&
			StringUtils.hasText(firstAuthor) &&
			StringUtils.hasText(bookDto.getPublisher())) {

			Optional<Book> existingByTitleAuthorPublisher =
				bookRepository.findByTitleAndAuthorAndPublisher(
					bookDto.getTitle(),
					firstAuthor,
					bookDto.getPublisher()
				);

			if (existingByTitleAuthorPublisher.isPresent()) {
				log.info("Found existing book by title/author/publisher: {}", bookDto.getTitle());
				return existingByTitleAuthorPublisher.get();
			}
		}

		// 3. 새로운 책 생성
		log.info("Creating new book: {}", bookDto.getTitle());
		return createNewBook(bookDto);
	}

	private Book createNewBook(BookDto bookDto) {
		Book book = Book.builder()
			.title(bookDto.getTitle())
			.author(getFirstAuthor(bookDto))
			.publisher(bookDto.getPublisher())
			.isbn(extractFirstIsbn(bookDto.getIsbn()))
			.description(bookDto.getContents())
			.coverImage(bookDto.getThumbnail())
			.publishedDate(bookDto.getPublishedDate())
			.build();

		Book savedBook = bookRepository.save(book);
		log.info("Created new book with ID: {}", savedBook.getBookId());

		return savedBook;
	}

	private String extractFirstIsbn(String isbn) {
		if (!StringUtils.hasText(isbn)) {
			return null;
		}

		String trimmedIsbn = isbn.trim();
		if (trimmedIsbn.contains(" ")) {
			return trimmedIsbn.split(" ")[0];
		}

		return trimmedIsbn;
	}

	private String getFirstAuthor(BookDto bookDto) {
		if (bookDto.getAuthors() == null || bookDto.getAuthors().isEmpty()) {
			return "";
		}
		return bookDto.getAuthors().get(0);
	}

	@Transactional(readOnly = true)
	public Book findBookById(Long bookId) {
		return bookRepository.findById(bookId)
			.orElseThrow(() -> new IllegalArgumentException("책을 찾을 수 없습니다: " + bookId));
	}

	@Transactional(readOnly = true)
	public Optional<Book> findBookByIsbn(String isbn) {
		String firstIsbn = extractFirstIsbn(isbn);
		if (!StringUtils.hasText(firstIsbn)) {
			return Optional.empty();
		}
		return bookRepository.findByIsbn(firstIsbn);
	}
}
