package com.example.seolab.integration.api;

import com.example.seolab.dto.request.AddQuoteRequest;
import com.example.seolab.dto.request.UpdateQuoteRequest;
import com.example.seolab.entity.Book;
import com.example.seolab.entity.Quote;
import com.example.seolab.entity.User;
import com.example.seolab.entity.UserBook;
import com.example.seolab.repository.BookRepository;
import com.example.seolab.repository.QuoteRepository;
import com.example.seolab.repository.UserBookRepository;
import com.example.seolab.repository.UserRepository;
import com.example.seolab.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Quote API 통합 테스트")
class QuoteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private UserBookRepository userBookRepository;

	@Autowired
	private QuoteRepository quoteRepository;

	@Autowired
	private JwtUtil jwtUtil;

	private User testUser;
	private String accessToken;
	private Book testBook;
	private UserBook testUserBook;

	@BeforeEach
	void setUp() {
		quoteRepository.deleteAll();
		userBookRepository.deleteAll();
		bookRepository.deleteAll();
		userRepository.deleteAll();

		testUser = User.builder()
			.email("test@example.com")
			.username("test")
			.passwordHash("encoded")
			.build();
		testUser = userRepository.save(testUser);

		accessToken = jwtUtil.generateAccessToken(testUser);

		testBook = Book.builder()
			.title("테스트 책")
			.authors(List.of("저자1"))
			.publisher("출판사")
			.isbn("1234567890")
			.build();
		testBook = bookRepository.save(testBook);

		testUserBook = UserBook.builder()
			.user(testUser)
			.book(testBook)
			.isReading(true)
			.build();
		testUserBook = userBookRepository.save(testUserBook);
	}

	@Test
	@DisplayName("POST /api/books/{userBookId}/quotes - 문장을 추가할 수 있다")
	void addQuote_withValidRequest_returnsCreated() throws Exception {
		// given
		AddQuoteRequest request = new AddQuoteRequest("인상 깊은 문장입니다.", 42);

		// when & then
		mockMvc.perform(post("/api/books/{userBookId}/quotes", testUserBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.quoteId").exists())
			.andExpect(jsonPath("$.text").value("인상 깊은 문장입니다."))
			.andExpect(jsonPath("$.page").value(42))
			.andExpect(jsonPath("$.isFavorite").value(false));
	}

	@Test
	@DisplayName("POST /api/books/{userBookId}/quotes - 인증 없이 요청하면 401을 반환한다")
	void addQuote_withoutAuthentication_returns401() throws Exception {
		// given
		AddQuoteRequest request = new AddQuoteRequest("문장", 1);

		// when & then
		mockMvc.perform(post("/api/books/{userBookId}/quotes", testUserBook.getUserBookId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("POST /api/books/{userBookId}/quotes - 다른 사용자의 책에 문장을 추가하면 403을 반환한다")
	void addQuote_withDifferentUser_returns403() throws Exception {
		// given
		User anotherUser = userRepository.save(User.builder()
			.email("other@example.com").username("other").passwordHash("pwd").build());
		UserBook otherUserBook = userBookRepository.save(UserBook.builder()
			.user(anotherUser).book(testBook).isReading(true).build());

		AddQuoteRequest request = new AddQuoteRequest("문장", 1);

		// when & then
		mockMvc.perform(post("/api/books/{userBookId}/quotes", otherUserBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value(containsString("접근할 수 없는 책")));
	}

	@Test
	@DisplayName("GET /api/books/{userBookId}/quotes - 해당 책의 모든 문장을 조회할 수 있다")
	void getQuotes_returnsAllQuotes() throws Exception {
		// given
		Quote quote1 = quoteRepository.save(Quote.builder()
			.userBook(testUserBook).text("문장1").page(1).build());
		Quote quote2 = quoteRepository.save(Quote.builder()
			.userBook(testUserBook).text("문장2").page(2).build());

		// when & then
		mockMvc.perform(get("/api/books/{userBookId}/quotes", testUserBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].text").value("문장1"))
			.andExpect(jsonPath("$[1].text").value("문장2"));
	}

	@Test
	@DisplayName("GET /api/books/{userBookId}/quotes?favorite=true - 즐겨찾기 문장만 조회할 수 있다")
	void getQuotes_withFavoriteFilter_returnsFavoriteQuotes() throws Exception {
		// given
		Quote favoriteQuote = quoteRepository.save(Quote.builder()
			.userBook(testUserBook).text("즐겨찾기").page(1).isFavorite(true).build());
		Quote normalQuote = quoteRepository.save(Quote.builder()
			.userBook(testUserBook).text("일반").page(2).isFavorite(false).build());

		// when & then
		mockMvc.perform(get("/api/books/{userBookId}/quotes", testUserBook.getUserBookId())
				.param("favorite", "true")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].text").value("즐겨찾기"))
			.andExpect(jsonPath("$[0].isFavorite").value(true));
	}

	@Test
	@DisplayName("GET /api/books/{userBookId}/quotes/{quoteId} - 특정 문장을 조회할 수 있다")
	void getQuote_returnsQuoteDetail() throws Exception {
		// given
		Quote quote = quoteRepository.save(Quote.builder()
			.userBook(testUserBook).text("특정 문장").page(10).build());

		// when & then
		mockMvc.perform(get("/api/books/{userBookId}/quotes/{quoteId}",
				testUserBook.getUserBookId(), quote.getQuoteId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.quoteId").value(quote.getQuoteId().toString()))
			.andExpect(jsonPath("$.text").value("특정 문장"))
			.andExpect(jsonPath("$.page").value(10));
	}

	@Test
	@DisplayName("PUT /api/books/{userBookId}/quotes/{quoteId} - 문장을 수정할 수 있다")
	void updateQuote_updatesQuoteSuccessfully() throws Exception {
		// given
		Quote quote = quoteRepository.save(Quote.builder()
			.userBook(testUserBook).text("원본 문장").page(5).build());

		UpdateQuoteRequest request = new UpdateQuoteRequest("수정된 문장", 15);

		// when & then
		mockMvc.perform(put("/api/books/{userBookId}/quotes/{quoteId}",
				testUserBook.getUserBookId(), quote.getQuoteId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.text").value("수정된 문장"))
			.andExpect(jsonPath("$.page").value(15));
	}

	@Test
	@DisplayName("PATCH /api/books/{userBookId}/quotes/{quoteId}/favorite - 즐겨찾기를 토글할 수 있다")
	void toggleFavorite_togglesSuccessfully() throws Exception {
		// given
		Quote quote = quoteRepository.save(Quote.builder()
			.userBook(testUserBook).text("문장").page(1).isFavorite(false).build());

		// when & then
		mockMvc.perform(patch("/api/books/{userBookId}/quotes/{quoteId}/favorite",
				testUserBook.getUserBookId(), quote.getQuoteId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isFavorite").value(true));
	}

	@Test
	@DisplayName("DELETE /api/books/{userBookId}/quotes/{quoteId} - 문장을 삭제할 수 있다")
	void deleteQuote_deletesSuccessfully() throws Exception {
		// given
		Quote quote = quoteRepository.save(Quote.builder()
			.userBook(testUserBook).text("삭제할 문장").page(1).build());

		// when & then
		mockMvc.perform(delete("/api/books/{userBookId}/quotes/{quoteId}",
				testUserBook.getUserBookId(), quote.getQuoteId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("POST /api/books/{userBookId}/quotes - 빈 문장은 추가할 수 없다")
	void addQuote_withEmptyText_returns400() throws Exception {
		// given
		AddQuoteRequest request = new AddQuoteRequest("", 1);

		// when & then
		mockMvc.perform(post("/api/books/{userBookId}/quotes", testUserBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(containsString("문장")));
	}

	@Test
	@DisplayName("POST /api/books/{userBookId}/quotes - 1000자를 초과하는 문장은 추가할 수 없다")
	void addQuote_withTooLongText_returns400() throws Exception {
		// given
		String longText = "a".repeat(1001);
		AddQuoteRequest request = new AddQuoteRequest(longText, 1);

		// when & then
		mockMvc.perform(post("/api/books/{userBookId}/quotes", testUserBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(containsString("1000자")));
	}

	@Test
	@DisplayName("GET /api/books/{userBookId}/quotes/{quoteId} - 다른 책의 문장을 조회하면 예외가 발생한다")
	void getQuote_withWrongUserBook_returns400() throws Exception {
		// given
		Book anotherBook = bookRepository.save(Book.builder()
			.title("다른 책").authors(List.of("저자")).publisher("출판사").isbn("9999999999").build());
		UserBook anotherUserBook = userBookRepository.save(UserBook.builder()
			.user(testUser).book(anotherBook).isReading(true).build());

		Quote quote = quoteRepository.save(Quote.builder()
			.userBook(testUserBook).text("문장").page(1).build());

		// when & then - 다른 userBook으로 접근
		mockMvc.perform(get("/api/books/{userBookId}/quotes/{quoteId}",
				anotherUserBook.getUserBookId(), quote.getQuoteId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(containsString("해당 책에 속하지 않는 문장")));
	}
}
