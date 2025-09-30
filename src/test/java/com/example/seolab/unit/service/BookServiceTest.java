package com.example.seolab.unit.service;

import com.example.seolab.dto.response.BookDto;
import com.example.seolab.entity.Book;
import com.example.seolab.repository.BookRepository;
import com.example.seolab.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 단위 테스트")
class BookServiceTest {

	@Mock
	private BookRepository bookRepository;

	@InjectMocks
	private BookService bookService;

	private BookDto bookDto;
	private Book existingBook;

	@BeforeEach
	void setUp() {
		bookDto = BookDto.builder()
			.title("테스트 책")
			.contents("테스트 내용")
			.isbn("1234567890 9876543210")
			.publishedDate(LocalDate.of(2024, 1, 1))
			.authors(List.of("저자1", "저자2"))
			.publisher("출판사")
			.translators(List.of("역자1"))
			.thumbnail("https://example.com/image.jpg")
			.build();

		existingBook = Book.builder()
			.bookId(1L)
			.title("테스트 책")
			.authors(List.of("저자1", "저자2"))
			.publisher("출판사")
			.isbn("1234567890")
			.contents("테스트 내용")
			.thumbnail("https://example.com/image.jpg")
			.publishedDate(LocalDate.of(2024, 1, 1))
			.translators(List.of("역자1"))
			.build();
	}

	@Test
	@DisplayName("ISBN으로 기존 책을 찾으면 새로 생성하지 않고 반환")
	void findOrCreateBook_withExistingIsbn_returnsExistingBook() {
		// given
		when(bookRepository.findByIsbn("1234567890"))
			.thenReturn(Optional.of(existingBook));

		// when
		Book result = bookService.findOrCreateBook(bookDto);

		// then
		assertThat(result).isEqualTo(existingBook);
		verify(bookRepository).findByIsbn("1234567890");
		verify(bookRepository, never()).save(any(Book.class));
	}

	@Test
	@DisplayName("ISBN이 없고 제목+저자+출판사로 기존 책을 찾으면 반환")
	void findOrCreateBook_withoutIsbnButMatchingTitleAuthorPublisher_returnsExistingBook() {
		// given
		BookDto bookDtoWithoutIsbn = BookDto.builder()
			.title("테스트 책")
			.authors(List.of("저자1", "저자2"))
			.publisher("출판사")
			.build();

		when(bookRepository.findByTitleAndAuthorAndPublisher("테스트 책", "저자1", "출판사"))
			.thenReturn(Optional.of(existingBook));

		// when
		Book result = bookService.findOrCreateBook(bookDtoWithoutIsbn);

		// then
		assertThat(result).isEqualTo(existingBook);
		verify(bookRepository).findByTitleAndAuthorAndPublisher("테스트 책", "저자1", "출판사");
		verify(bookRepository, never()).save(any(Book.class));
	}

	@Test
	@DisplayName("기존 책이 없으면 새로운 책을 생성")
	void findOrCreateBook_withNewBook_createsAndReturnsNewBook() {
		// given
		when(bookRepository.findByIsbn("1234567890"))
			.thenReturn(Optional.empty());
		when(bookRepository.save(any(Book.class)))
			.thenReturn(existingBook);

		// when
		Book result = bookService.findOrCreateBook(bookDto);

		// then
		assertThat(result).isNotNull();

		ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
		verify(bookRepository).save(bookCaptor.capture());

		Book savedBook = bookCaptor.getValue();
		assertThat(savedBook.getTitle()).isEqualTo("테스트 책");
		assertThat(savedBook.getAuthors()).containsExactly("저자1", "저자2");
		assertThat(savedBook.getPublisher()).isEqualTo("출판사");
		assertThat(savedBook.getIsbn()).isEqualTo("1234567890"); // 첫 번째 ISBN만 저장
	}

	@Test
	@DisplayName("ISBN에서 첫 번째 값만 추출하여 저장")
	void findOrCreateBook_extractsFirstIsbn() {
		// given
		when(bookRepository.findByIsbn("1234567890"))
			.thenReturn(Optional.empty());
		when(bookRepository.save(any(Book.class)))
			.thenReturn(existingBook);

		// when
		bookService.findOrCreateBook(bookDto);

		// then
		ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
		verify(bookRepository).save(bookCaptor.capture());

		Book savedBook = bookCaptor.getValue();
		assertThat(savedBook.getIsbn()).isEqualTo("1234567890");
		assertThat(savedBook.getIsbn()).doesNotContain(" ");
	}

	@Test
	@DisplayName("ISBN이 있으면 제목+저자+출판사 조회를 하지 않음")
	void findOrCreateBook_withIsbn_doesNotSearchByTitleAuthorPublisher() {
		// given
		when(bookRepository.findByIsbn("1234567890"))
			.thenReturn(Optional.empty());
		when(bookRepository.save(any(Book.class)))
			.thenReturn(existingBook);

		// when
		bookService.findOrCreateBook(bookDto);

		// then
		verify(bookRepository).findByIsbn("1234567890");
		verify(bookRepository, never())
			.findByTitleAndAuthorAndPublisher(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("저자 리스트가 비어있으면 빈 리스트로 저장")
	void findOrCreateBook_withEmptyAuthors_savesEmptyList() {
		// given
		BookDto bookWithNoAuthors = BookDto.builder()
			.title("저자 없는 책")
			.isbn("1111111111")
			.publisher("출판사")
			.authors(null)
			.build();

		when(bookRepository.findByIsbn("1111111111"))
			.thenReturn(Optional.empty());
		when(bookRepository.save(any(Book.class)))
			.thenReturn(existingBook);

		// when
		bookService.findOrCreateBook(bookWithNoAuthors);

		// then
		ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
		verify(bookRepository).save(bookCaptor.capture());

		Book savedBook = bookCaptor.getValue();
		assertThat(savedBook.getAuthors()).isEmpty();
	}

	@Test
	@DisplayName("번역자 리스트가 비어있으면 빈 리스트로 저장")
	void findOrCreateBook_withEmptyTranslators_savesEmptyList() {
		// given
		BookDto bookWithNoTranslators = BookDto.builder()
			.title("번역자 없는 책")
			.isbn("2222222222")
			.publisher("출판사")
			.authors(List.of("저자"))
			.translators(null)
			.build();

		when(bookRepository.findByIsbn("2222222222"))
			.thenReturn(Optional.empty());
		when(bookRepository.save(any(Book.class)))
			.thenReturn(existingBook);

		// when
		bookService.findOrCreateBook(bookWithNoTranslators);

		// then
		ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
		verify(bookRepository).save(bookCaptor.capture());

		Book savedBook = bookCaptor.getValue();
		assertThat(savedBook.getTranslators()).isEmpty();
	}
}
