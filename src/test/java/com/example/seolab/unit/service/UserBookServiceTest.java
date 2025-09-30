package com.example.seolab.unit.service;

import com.example.seolab.dto.request.AddBookRequest;
import com.example.seolab.dto.response.AddBookResponse;
import com.example.seolab.dto.response.BookDto;
import com.example.seolab.dto.response.UserBookResponse;
import com.example.seolab.entity.Book;
import com.example.seolab.entity.User;
import com.example.seolab.entity.UserBook;
import com.example.seolab.exception.AccessDeniedException;
import com.example.seolab.exception.DuplicateBookException;
import com.example.seolab.repository.QuoteRepository;
import com.example.seolab.repository.UserBookRepository;
import com.example.seolab.repository.UserRepository;
import com.example.seolab.service.BookService;
import com.example.seolab.service.UserBookService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserBookService 단위 테스트")
class UserBookServiceTest {

	@Mock
	private UserBookRepository userBookRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private BookService bookService;

	@Mock
	private QuoteRepository quoteRepository;

	@InjectMocks
	private UserBookService userBookService;

	private User testUser;
	private Book testBook;
	private UserBook testUserBook;
	private AddBookRequest addBookRequest;
	private UUID userBookId;

	@BeforeEach
	void setUp() {
		testUser = User.builder()
			.userId(1L)
			.email("test@example.com")
			.username("test")
			.passwordHash("password")
			.build();

		testBook = Book.builder()
			.bookId(1L)
			.title("테스트 책")
			.authors(List.of("저자1"))
			.publisher("출판사")
			.isbn("1234567890")
			.build();

		userBookId = UUID.randomUUID();
		testUserBook = UserBook.builder()
			.userBookId(userBookId)
			.user(testUser)
			.book(testBook)
			.isReading(true)
			.isFavorite(false)
			.build();

		addBookRequest = new AddBookRequest(
			"테스트 책",
			"테스트 내용",
			"1234567890",
			"2024-01-01T00:00:00.000+09:00",
			List.of("저자1"),
			"출판사",
			List.of("역자1"),
			"https://example.com/thumbnail.jpg"
		);
	}

	@Test
	@DisplayName("새로운 책을 사용자 서재에 추가하면 UserBook이 생성")
	void addBookToUserLibrary_withNewBook_createsUserBook() {
		// given
		when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
		when(bookService.findOrCreateBook(any(BookDto.class))).thenReturn(testBook);
		when(userBookRepository.findByUserUserIdAndBookBookId(1L, 1L))
			.thenReturn(Optional.empty());
		when(userBookRepository.save(any(UserBook.class))).thenReturn(testUserBook);

		// when
		AddBookResponse result = userBookService.addBookToUserLibrary(1L, addBookRequest);

		// then
		assertThat(result.getUserBookId()).isEqualTo(userBookId);
		assertThat(result.getMessage()).contains("성공적으로 추가");

		ArgumentCaptor<UserBook> userBookCaptor = ArgumentCaptor.forClass(UserBook.class);
		verify(userBookRepository).save(userBookCaptor.capture());

		UserBook savedUserBook = userBookCaptor.getValue();
		assertThat(savedUserBook.getUser()).isEqualTo(testUser);
		assertThat(savedUserBook.getBook()).isEqualTo(testBook);
		assertThat(savedUserBook.getIsReading()).isTrue();
		assertThat(savedUserBook.getIsFavorite()).isFalse();
	}

	@Test
	@DisplayName("이미 추가된 책을 다시 추가하면 DuplicateBookException이 발생")
	void addBookToUserLibrary_withDuplicateBook_throwsDuplicateBookException() {
		// given
		when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
		when(bookService.findOrCreateBook(any(BookDto.class))).thenReturn(testBook);
		when(userBookRepository.findByUserUserIdAndBookBookId(1L, 1L))
			.thenReturn(Optional.of(testUserBook));

		// when & then
		assertThatThrownBy(() -> userBookService.addBookToUserLibrary(1L, addBookRequest))
			.isInstanceOf(DuplicateBookException.class)
			.hasMessageContaining("이미 내 서재에 추가된 책")
			.satisfies(ex -> {
				DuplicateBookException dbe = (DuplicateBookException) ex;
				assertThat(dbe.getUserBookId()).isEqualTo(userBookId);
			});

		verify(userBookRepository, never()).save(any(UserBook.class));
	}

	@Test
	@DisplayName("존재하지 않는 사용자가 책을 추가하면 예외가 발생")
	void addBookToUserLibrary_withNonExistentUser_throwsException() {
		// given
		when(userRepository.findById(999L)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> userBookService.addBookToUserLibrary(999L, addBookRequest))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("사용자를 찾을 수 없습니다");
	}

	@Test
	@DisplayName("책을 완독 처리하면 isReading이 false로 변경되고 endDate가 설정")
	void markBookAsCompleted_changesReadingStatusAndSetsEndDate() {
		// given
		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(userBookRepository.save(any(UserBook.class)))
			.thenReturn(testUserBook);

		// when
		UserBookResponse result = userBookService.markBookAsCompleted(1L, userBookId);

		// then
		verify(userBookRepository).save(testUserBook);
		assertThat(testUserBook.getIsReading()).isFalse();
		assertThat(testUserBook.getEndDate()).isNotNull();
	}

	@Test
	@DisplayName("완독된 책을 다시 읽는 중으로 변경하면 endDate가 제거됨")
	void markBookAsCompleted_togglesBackToReading_removesEndDate() {
		// given
		testUserBook.setIsReading(false);
		testUserBook.setEndDate(LocalDate.now());

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(userBookRepository.save(any(UserBook.class)))
			.thenReturn(testUserBook);

		// when
		userBookService.markBookAsCompleted(1L, userBookId);

		// then
		assertThat(testUserBook.getIsReading()).isTrue();
		assertThat(testUserBook.getEndDate()).isNull();
	}

	@Test
	@DisplayName("즐겨찾기를 토글하면 isFavorite 값이 반전됨")
	void toggleFavorite_togglesFavoriteStatus() {
		// given
		assertThat(testUserBook.getIsFavorite()).isFalse();

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(userBookRepository.save(any(UserBook.class)))
			.thenReturn(testUserBook);

		// when
		userBookService.toggleFavorite(1L, userBookId);

		// then
		assertThat(testUserBook.getIsFavorite()).isTrue();
	}

	@Test
	@DisplayName("다른 사용자의 책에 접근하면 AccessDeniedException이 발생")
	void getUserBook_withDifferentUser_throwsAccessDeniedException() {
		// given
		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));

		// when & then
		assertThatThrownBy(() -> userBookService.getUserBook(999L, userBookId))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("접근할 수 없는 책");
	}

	@Test
	@DisplayName("사용자의 모든 책을 조회 가능")
	void getUserBooks_returnsAllUserBooks() {
		// given
		List<UserBook> userBooks = List.of(testUserBook);
		when(userBookRepository.findByUserUserIdOrderByUpdatedAtDesc(1L))
			.thenReturn(userBooks);
		when(quoteRepository.countByUserBookUserBookId(userBookId))
			.thenReturn(5L);

		// when
		List<UserBookResponse> result = userBookService.getUserBooks(1L, null, null);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getUserBookId()).isEqualTo(userBookId);
		assertThat(result.get(0).getQuoteCount()).isEqualTo(5L);
	}

	@Test
	@DisplayName("즐겨찾기 책만 필터링하여 조회 가능")
	void getUserBooks_withFavoriteFilter_returnsFavoriteBooks() {
		// given
		testUserBook.setIsFavorite(true);
		List<UserBook> favoriteBooks = List.of(testUserBook);

		when(userBookRepository.findByUserUserIdAndIsFavoriteTrueOrderByUpdatedAtDesc(1L))
			.thenReturn(favoriteBooks);
		when(quoteRepository.countByUserBookUserBookId(userBookId))
			.thenReturn(3L);

		// when
		List<UserBookResponse> result = userBookService.getUserBooks(1L, true, null);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getIsFavorite()).isTrue();
		verify(userBookRepository).findByUserUserIdAndIsFavoriteTrueOrderByUpdatedAtDesc(1L);
	}

	@Test
	@DisplayName("읽는 중인 책만 필터링하여 조회 가능")
	void getUserBooks_withReadingFilter_returnsReadingBooks() {
		// given
		List<UserBook> readingBooks = List.of(testUserBook);

		when(userBookRepository.findByUserUserIdAndIsReadingOrderByUpdatedAtDesc(1L, true))
			.thenReturn(readingBooks);
		when(quoteRepository.countByUserBookUserBookId(userBookId))
			.thenReturn(2L);

		// when
		List<UserBookResponse> result = userBookService.getUserBooks(1L, null, true);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getIsReading()).isTrue();
		verify(userBookRepository).findByUserUserIdAndIsReadingOrderByUpdatedAtDesc(1L, true);
	}

	@Test
	@DisplayName("UserBook 삭제 시 관련된 모든 Quote도 함께 삭제됨")
	void deleteUserBook_deletesUserBookAndQuotes() {
		// given
		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(quoteRepository.findByUserBookUserBookIdOrderByCreatedAtAsc(userBookId))
			.thenReturn(List.of());

		// when
		userBookService.deleteUserBook(1L, userBookId);

		// then
		verify(quoteRepository).findByUserBookUserBookIdOrderByCreatedAtAsc(userBookId);
		verify(userBookRepository).delete(testUserBook);
	}
}
