package com.example.seolab.integration.repository;

import com.example.seolab.entity.Book;
import com.example.seolab.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("BookRepository 통합 테스트")
class BookRepositoryTest {

	@Autowired
	private BookRepository bookRepository;

	private Book testBook;

	@BeforeEach
	void setUp() {
		bookRepository.deleteAll();

		testBook = Book.builder()
			.title("테스트 책")
			.authors(List.of("저자1", "저자2"))
			.publisher("테스트 출판사")
			.isbn("1234567890")
			.contents("책 소개")
			.thumbnail("https://example.com/thumbnail.jpg")
			.publishedDate(LocalDate.of(2024, 1, 1))
			.translators(List.of("역자1"))
			.build();
	}

	@Test
	@DisplayName("책을 저장하고 조회할 수 있다")
	void saveAndFindBook() {
		// when
		Book savedBook = bookRepository.save(testBook);

		// then
		assertThat(savedBook.getBookId()).isNotNull();
		assertThat(savedBook.getTitle()).isEqualTo("테스트 책");
		assertThat(savedBook.getAuthors()).containsExactly("저자1", "저자2");
		assertThat(savedBook.getPublisher()).isEqualTo("테스트 출판사");
		assertThat(savedBook.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("ISBN으로 책을 찾을 수 있다")
	void findByIsbn_returnsBook() {
		// given
		bookRepository.save(testBook);

		// when
		Optional<Book> found = bookRepository.findByIsbn("1234567890");

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getTitle()).isEqualTo("테스트 책");
	}

	@Test
	@DisplayName("ISBN 존재 여부를 확인할 수 있다")
	void existsByIsbn_returnsTrue() {
		// given
		bookRepository.save(testBook);

		// when
		boolean exists = bookRepository.existsByIsbn("1234567890");

		// then
		assertThat(exists).isTrue();
	}

	@Test
	@Disabled("H2는 JSON_CONTAINS를 지원하지 않음 - MySQL 환경에서만 테스트")
	@DisplayName("제목, 저자, 출판사로 책을 찾을 수 있다 (JSON 쿼리)")
	void findByTitleAndAuthorAndPublisher_returnsBook() {
		// given
		bookRepository.save(testBook);

		// when
		Optional<Book> found = bookRepository.findByTitleAndAuthorAndPublisher(
			"테스트 책",
			"저자1",
			"테스트 출판사"
		);

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getTitle()).isEqualTo("테스트 책");
		assertThat(found.get().getAuthors()).contains("저자1");
	}

	@Test
	@Disabled("H2는 JSON_CONTAINS를 지원하지 않음 - MySQL 환경에서만 테스트")
	@DisplayName("저자 배열에 포함된 저자로 검색할 수 있다 (JSON_CONTAINS)")
	void findByTitleAndAuthorAndPublisher_withSecondAuthor_returnsBook() {
		// given
		bookRepository.save(testBook);

		// when
		Optional<Book> found = bookRepository.findByTitleAndAuthorAndPublisher(
			"테스트 책",
			"저자2",  // 두 번째 저자로 검색
			"테스트 출판사"
		);

		// then
		assertThat(found).isPresent();
		assertThat(found.get().getAuthors()).contains("저자2");
	}

	@Test
	@Disabled("H2는 JSON_CONTAINS를 지원하지 않음 - MySQL 환경에서만 테스트")
	@DisplayName("제목, 저자, 출판사 조합의 존재 여부를 확인할 수 있다")
	void existsByTitleAndAuthorAndPublisher_returnsTrue() {
		// given
		bookRepository.save(testBook);

		// when
		boolean exists = bookRepository.existsByTitleAndAuthorAndPublisher(
			"테스트 책",
			"저자1",
			"테스트 출판사"
		);

		// then
		assertThat(exists).isTrue();
	}

	@Test
	@Disabled("H2는 JSON_CONTAINS를 지원하지 않음 - MySQL 환경에서만 테스트")
	@DisplayName("ISBN 또는 제목+저자+출판사로 책을 찾을 수 있다")
	void findByIsbnOrTitleAndAuthorAndPublisher_withIsbn_returnsBook() {
		// given
		bookRepository.save(testBook);

		// when - ISBN으로 검색
		Optional<Book> foundByIsbn = bookRepository.findByIsbnOrTitleAndAuthorAndPublisher(
			"1234567890",
			null,
			null,
			null
		);

		// then
		assertThat(foundByIsbn).isPresent();
		assertThat(foundByIsbn.get().getTitle()).isEqualTo("테스트 책");
	}

	@Test
	@Disabled("H2는 JSON_CONTAINS를 지원하지 않음 - MySQL 환경에서만 테스트")
	@DisplayName("ISBN이 없을 때는 제목+저자+출판사로 책을 찾는다")
	void findByIsbnOrTitleAndAuthorAndPublisher_withoutIsbn_returnsBook() {
		// given
		bookRepository.save(testBook);

		// when - 제목+저자+출판사로 검색
		Optional<Book> foundByTitleAuthorPublisher = bookRepository.findByIsbnOrTitleAndAuthorAndPublisher(
			null,
			"테스트 책",
			"저자1",
			"테스트 출판사"
		);

		// then
		assertThat(foundByTitleAuthorPublisher).isPresent();
		assertThat(foundByTitleAuthorPublisher.get().getIsbn()).isEqualTo("1234567890");
	}

	@Test
	@DisplayName("저자가 여러 명인 책을 저장하고 조회할 수 있다")
	void saveBook_withMultipleAuthors() {
		// given
		Book multiAuthorBook = Book.builder()
			.title("공저")
			.authors(List.of("저자A", "저자B", "저자C"))
			.publisher("출판사")
			.isbn("9999999999")
			.build();

		// when
		Book saved = bookRepository.save(multiAuthorBook);

		// then
		assertThat(saved.getAuthors()).hasSize(3);
		assertThat(saved.getAuthors()).containsExactly("저자A", "저자B", "저자C");
	}

	@Test
	@DisplayName("저자가 없는 책도 저장할 수 있다")
	void saveBook_withNoAuthors() {
		// given
		Book noAuthorBook = Book.builder()
			.title("저자 미상")
			.authors(List.of())
			.publisher("출판사")
			.isbn("0000000000")
			.build();

		// when
		Book saved = bookRepository.save(noAuthorBook);

		// then
		assertThat(saved.getAuthors()).isEmpty();
	}

	@Test
	@DisplayName("번역자 정보를 포함한 책을 저장하고 조회할 수 있다")
	void saveBook_withTranslators() {
		// when
		Book saved = bookRepository.save(testBook);

		// then
		assertThat(saved.getTranslators()).hasSize(1);
		assertThat(saved.getTranslators()).contains("역자1");
	}

	@Test
	@DisplayName("같은 제목이지만 저자가 다른 책은 별도로 저장된다")
	void saveDifferentBooks_withSameTitle() {
		// given
		Book book1 = Book.builder()
			.title("같은 제목")
			.authors(List.of("저자A"))
			.publisher("출판사1")
			.isbn("1111111111")
			.build();

		Book book2 = Book.builder()
			.title("같은 제목")
			.authors(List.of("저자B"))
			.publisher("출판사2")
			.isbn("2222222222")
			.build();

		// when
		bookRepository.save(book1);
		bookRepository.save(book2);

		// then
		List<Book> allBooks = bookRepository.findAll();
		assertThat(allBooks).hasSize(2);
	}
}
