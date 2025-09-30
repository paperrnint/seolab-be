package com.example.seolab.unit.service;

import com.example.seolab.dto.request.AddQuoteRequest;
import com.example.seolab.dto.request.UpdateQuoteRequest;
import com.example.seolab.dto.response.QuoteResponse;
import com.example.seolab.entity.Book;
import com.example.seolab.entity.Quote;
import com.example.seolab.entity.User;
import com.example.seolab.entity.UserBook;
import com.example.seolab.exception.AccessDeniedException;
import com.example.seolab.repository.QuoteRepository;
import com.example.seolab.repository.UserBookRepository;
import com.example.seolab.service.QuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuoteService 단위 테스트")
class QuoteServiceTest {

	@Mock
	private QuoteRepository quoteRepository;

	@Mock
	private UserBookRepository userBookRepository;

	@InjectMocks
	private QuoteService quoteService;

	private User testUser;
	private Book testBook;
	private UserBook testUserBook;
	private Quote testQuote;
	private UUID userBookId;
	private UUID quoteId;

	@BeforeEach
	void setUp() {
		testUser = User.builder()
			.userId(1L)
			.email("test@example.com")
			.username("test")
			.build();

		testBook = Book.builder()
			.bookId(1L)
			.title("테스트 책")
			.authors(List.of("저자1"))
			.publisher("출판사")
			.build();

		userBookId = UUID.randomUUID();
		testUserBook = UserBook.builder()
			.userBookId(userBookId)
			.user(testUser)
			.book(testBook)
			.isReading(true)
			.build();

		quoteId = UUID.randomUUID();
		testQuote = Quote.builder()
			.quoteId(quoteId)
			.userBook(testUserBook)
			.text("인상 깊은 문장")
			.page(42)
			.isFavorite(false)
			.build();
	}

	@Test
	@DisplayName("문장을 추가하면 Quote가 생성되고 UserBook의 updatedAt이 갱신")
	void addQuote_createsQuoteAndUpdatesUserBook() {
		// given
		AddQuoteRequest request = new AddQuoteRequest("새로운 문장", 10);

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(quoteRepository.save(any(Quote.class)))
			.thenReturn(testQuote);

		// when
		QuoteResponse result = quoteService.addQuote(1L, userBookId, request);

		// then
		assertThat(result).isNotNull();

		ArgumentCaptor<Quote> quoteCaptor = ArgumentCaptor.forClass(Quote.class);
		verify(quoteRepository).save(quoteCaptor.capture());

		Quote savedQuote = quoteCaptor.getValue();
		assertThat(savedQuote.getUserBook()).isEqualTo(testUserBook);
		assertThat(savedQuote.getText()).isEqualTo("새로운 문장");
		assertThat(savedQuote.getPage()).isEqualTo(10);
		assertThat(savedQuote.getIsFavorite()).isFalse();

		// UserBook의 활동 시간이 갱신되었는지 확인
		verify(userBookRepository).save(testUserBook);
	}

	@Test
	@DisplayName("다른 사용자의 UserBook에 문장을 추가하면 예외가 발생")
	void addQuote_withDifferentUser_throwsAccessDeniedException() {
		// given
		AddQuoteRequest request = new AddQuoteRequest("문장", 1);

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));

		// when & then
		assertThatThrownBy(() -> quoteService.addQuote(999L, userBookId, request))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("접근할 수 없는 책");
	}

	@Test
	@DisplayName("특정 책의 모든 문장을 조회할 수 있다")
	void getQuotesByUserBook_returnsAllQuotes() {
		// given
		List<Quote> quotes = List.of(testQuote);

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(quoteRepository.findByUserBookUserBookIdOrderByCreatedAtAsc(userBookId))
			.thenReturn(quotes);

		// when
		List<QuoteResponse> result = quoteService.getQuotesByUserBook(1L, userBookId, null);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getQuoteId()).isEqualTo(quoteId);
		assertThat(result.get(0).getText()).isEqualTo("인상 깊은 문장");
	}

	@Test
	@DisplayName("즐겨찾기 문장만 필터링하여 조회 가능")
	void getQuotesByUserBook_withFavoriteFilter_returnsFavoriteQuotes() {
		// given
		testQuote.setIsFavorite(true);
		List<Quote> favoriteQuotes = List.of(testQuote);

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(quoteRepository.findByUserBookUserBookIdAndIsFavoriteTrueOrderByCreatedAtAsc(userBookId))
			.thenReturn(favoriteQuotes);

		// when
		List<QuoteResponse> result = quoteService.getQuotesByUserBook(1L, userBookId, true);

		// then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getIsFavorite()).isTrue();
		verify(quoteRepository).findByUserBookUserBookIdAndIsFavoriteTrueOrderByCreatedAtAsc(userBookId);
	}

	@Test
	@DisplayName("문장을 수정하면 내용이 변경되고 UserBook의 updatedAt이 갱신")
	void updateQuoteByUserBook_updatesQuoteAndUserBook() {
		// given
		UpdateQuoteRequest request = new UpdateQuoteRequest("수정된 문장", 20);

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(quoteRepository.findByQuoteIdAndUserId(quoteId, 1L))
			.thenReturn(Optional.of(testQuote));
		when(quoteRepository.save(any(Quote.class)))
			.thenReturn(testQuote);

		// when
		QuoteResponse result = quoteService.updateQuoteByUserBook(1L, userBookId, quoteId, request);

		// then
		assertThat(testQuote.getText()).isEqualTo("수정된 문장");
		assertThat(testQuote.getPage()).isEqualTo(20);

		verify(quoteRepository).save(testQuote);
		verify(userBookRepository).save(testUserBook);
	}

	@Test
	@DisplayName("다른 UserBook의 문장을 수정하려고 하면 예외가 발생")
	void updateQuoteByUserBook_withDifferentUserBook_throwsException() {
		// given
		UUID anotherUserBookId = UUID.randomUUID();
		UserBook anotherUserBook = UserBook.builder()
			.userBookId(anotherUserBookId)
			.user(testUser)
			.book(testBook)
			.build();

		Quote quoteFromAnotherBook = Quote.builder()
			.quoteId(quoteId)
			.userBook(anotherUserBook)
			.text("다른 책의 문장")
			.build();

		UpdateQuoteRequest request = new UpdateQuoteRequest("수정", 1);

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(quoteRepository.findByQuoteIdAndUserId(quoteId, 1L))
			.thenReturn(Optional.of(quoteFromAnotherBook));

		// when & then
		assertThatThrownBy(() -> quoteService.updateQuoteByUserBook(1L, userBookId, quoteId, request))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("해당 책에 속하지 않는 문장");
	}

	@Test
	@DisplayName("즐겨찾기를 토글하면 isFavorite 값이 반전")
	void toggleQuoteFavoriteByUserBook_togglesFavoriteStatus() {
		// given
		assertThat(testQuote.getIsFavorite()).isFalse();

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(quoteRepository.findByQuoteIdAndUserId(quoteId, 1L))
			.thenReturn(Optional.of(testQuote));
		when(quoteRepository.save(any(Quote.class)))
			.thenReturn(testQuote);

		// when
		QuoteResponse result = quoteService.toggleQuoteFavoriteByUserBook(1L, userBookId, quoteId);

		// then
		assertThat(testQuote.getIsFavorite()).isTrue();
		assertThat(result.getIsFavorite()).isTrue();
		verify(quoteRepository).save(testQuote);
	}

	@Test
	@DisplayName("문장을 삭제하면 Quote가 삭제되고 UserBook의 updatedAt이 갱신")
	void deleteQuoteByUserBook_deletesQuoteAndUpdatesUserBook() {
		// given
		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(quoteRepository.findByQuoteIdAndUserId(quoteId, 1L))
			.thenReturn(Optional.of(testQuote));

		// when
		quoteService.deleteQuoteByUserBook(1L, userBookId, quoteId);

		// then
		verify(quoteRepository).delete(testQuote);
		verify(userBookRepository).save(testUserBook);
	}

	@Test
	@DisplayName("다른 사용자의 문장을 삭제하려고 하면 예외가 발생")
	void deleteQuoteByUserBook_withDifferentUser_throwsAccessDeniedException() {
		// given
		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));

		// when & then
		assertThatThrownBy(() -> quoteService.deleteQuoteByUserBook(999L, userBookId, quoteId))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessageContaining("접근할 수 없는 책");
	}

	@Test
	@DisplayName("문장 텍스트의 앞뒤 공백이 제거되어 저장")
	void addQuote_trimsWhitespace() {
		// given
		AddQuoteRequest request = new AddQuoteRequest("  공백이 있는 문장  ", 5);

		when(userBookRepository.findById(userBookId))
			.thenReturn(Optional.of(testUserBook));
		when(quoteRepository.save(any(Quote.class)))
			.thenReturn(testQuote);

		// when
		quoteService.addQuote(1L, userBookId, request);

		// then
		ArgumentCaptor<Quote> quoteCaptor = ArgumentCaptor.forClass(Quote.class);
		verify(quoteRepository).save(quoteCaptor.capture());

		Quote savedQuote = quoteCaptor.getValue();
		assertThat(savedQuote.getText()).isEqualTo("공백이 있는 문장");
		assertThat(savedQuote.getText()).doesNotStartWith(" ");
		assertThat(savedQuote.getText()).doesNotEndWith(" ");
	}
}
