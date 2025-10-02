package com.example.seolab.integration.api;

import com.example.seolab.dto.request.AddBookRequest;
import com.example.seolab.entity.Book;
import com.example.seolab.entity.User;
import com.example.seolab.entity.UserBook;
import com.example.seolab.repository.BookRepository;
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
@DisplayName("Book API 통합 테스트")
class BookControllerTest {

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
	private JwtUtil jwtUtil;

	private User testUser;
	private String accessToken;
	private Book testBook;

	@BeforeEach
	void setUp() {
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
			.title("기존 책")
			.authors(List.of("저자1"))
			.publisher("출판사")
			.isbn("1234567890")
			.build();
		testBook = bookRepository.save(testBook);
	}

	@Test
	@DisplayName("POST /api/books - 새로운 책을 서재에 추가할 수 있다")
	void addBook_withValidRequest_returnsCreated() throws Exception {
		// given
		AddBookRequest request = new AddBookRequest(
			"새 책",
			"내용",
			"9999999999",
			"2024-01-01T00:00:00.000+09:00",
			List.of("새 저자"),
			"새 출판사",
			List.of(),
			"https://example.com/thumb.jpg"
		);

		// when & then
		mockMvc.perform(post("/api/books")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.userBookId").exists())
			.andExpect(jsonPath("$.message").value(containsString("성공적으로 추가")));
	}

	@Test
	@DisplayName("POST /api/books - 이미 추가된 책을 다시 추가하면 409를 반환한다")
	void addBook_withDuplicateBook_returns409() throws Exception {
		// given - 먼저 책을 추가
		UserBook userBook = UserBook.builder()
			.user(testUser)
			.book(testBook)
			.isReading(true)
			.build();
		userBookRepository.save(userBook);

		AddBookRequest request = new AddBookRequest(
			"기존 책",
			"내용",
			"1234567890",
			"2024-01-01T00:00:00.000+09:00",
			List.of("저자1"),
			"출판사",
			List.of(),
			null
		);

		// when & then
		mockMvc.perform(post("/api/books")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.userBookId").exists())
			.andExpect(jsonPath("$.message").value(containsString("이미 내 서재에 추가된 책")));
	}

	@Test
	@DisplayName("POST /api/books - 인증 없이 요청하면 401을 반환한다")
	void addBook_withoutAuthentication_returns401() throws Exception {
		// given
		AddBookRequest request = new AddBookRequest(
			"책", null, null, null, List.of("저자"), "출판사", List.of(), null);

		// when & then
		mockMvc.perform(post("/api/books")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andDo(print())
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("GET /api/books - 사용자의 모든 책을 조회할 수 있다")
	void getUserBooks_returnsAllBooks() throws Exception {
		// given
		UserBook userBook1 = UserBook.builder()
			.user(testUser).book(testBook).isReading(true).build();
		userBookRepository.save(userBook1);

		Book book2 = bookRepository.save(Book.builder()
			.title("책2").authors(List.of("저자2")).publisher("출판사2").isbn("2222222222").build());
		UserBook userBook2 = UserBook.builder()
			.user(testUser).book(book2).isReading(false).build();
		userBookRepository.save(userBook2);

		// when & then
		mockMvc.perform(get("/api/books")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].book.title").exists())
			.andExpect(jsonPath("$[0].quoteCount").exists());
	}

	@Test
	@DisplayName("GET /api/books?favorite=true - 즐겨찾기 책만 조회할 수 있다")
	void getUserBooks_withFavoriteFilter_returnsFavoriteBooks() throws Exception {
		// given
		UserBook favoriteBook = UserBook.builder()
			.user(testUser).book(testBook).isReading(true).isFavorite(true).build();
		userBookRepository.save(favoriteBook);

		Book normalBook = bookRepository.save(Book.builder()
			.title("일반 책").authors(List.of("저자")).publisher("출판사").isbn("3333333333").build());
		UserBook userBook2 = UserBook.builder()
			.user(testUser).book(normalBook).isReading(true).isFavorite(false).build();
		userBookRepository.save(userBook2);

		// when & then
		mockMvc.perform(get("/api/books")
				.param("favorite", "true")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].isFavorite").value(true));
	}

	@Test
	@DisplayName("GET /api/books?reading=true - 읽는 중인 책만 조회할 수 있다")
	void getUserBooks_withReadingFilter_returnsReadingBooks() throws Exception {
		// given
		UserBook readingBook = UserBook.builder()
			.user(testUser).book(testBook).isReading(true).build();
		userBookRepository.save(readingBook);

		Book completedBook = bookRepository.save(Book.builder()
			.title("완독 책").authors(List.of("저자")).publisher("출판사").isbn("4444444444").build());
		UserBook userBook2 = UserBook.builder()
			.user(testUser).book(completedBook).isReading(false).build();
		userBookRepository.save(userBook2);

		// when & then
		mockMvc.perform(get("/api/books")
				.param("reading", "true")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].isReading").value(true));
	}

	@Test
	@DisplayName("GET /api/books/{userBookId} - 특정 책의 상세 정보를 조회할 수 있다")
	void getUserBook_returnsBookDetail() throws Exception {
		// given
		UserBook userBook = UserBook.builder()
			.user(testUser).book(testBook).isReading(true).build();
		userBook = userBookRepository.save(userBook);

		// when & then
		mockMvc.perform(get("/api/books/{userBookId}", userBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userBookId").value(userBook.getUserBookId().toString()))
			.andExpect(jsonPath("$.book.title").value("기존 책"))
			.andExpect(jsonPath("$.isReading").value(true));
	}

	@Test
	@DisplayName("GET /api/books/{userBookId} - 다른 사용자의 책에 접근하면 403을 반환한다")
	void getUserBook_withDifferentUser_returns403() throws Exception {
		// given
		User anotherUser = userRepository.save(User.builder()
			.email("other@example.com").username("other").passwordHash("pwd").build());
		UserBook otherUserBook = userBookRepository.save(UserBook.builder()
			.user(anotherUser).book(testBook).isReading(true).build());

		// when & then
		mockMvc.perform(get("/api/books/{userBookId}", otherUserBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value(containsString("접근할 수 없는 책")));
	}

	@Test
	@DisplayName("PATCH /api/books/{userBookId}/complete - 책을 완독 처리할 수 있다")
	void markBookAsCompleted_togglesReadingStatus() throws Exception {
		// given
		UserBook userBook = userBookRepository.save(UserBook.builder()
			.user(testUser).book(testBook).isReading(true).build());

		// when & then
		mockMvc.perform(patch("/api/books/{userBookId}/complete", userBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isReading").value(false))
			.andExpect(jsonPath("$.endDate").exists());
	}

	@Test
	@DisplayName("PATCH /api/books/{userBookId}/favorite - 즐겨찾기를 토글할 수 있다")
	void toggleFavorite_changesFavoriteStatus() throws Exception {
		// given
		UserBook userBook = userBookRepository.save(UserBook.builder()
			.user(testUser).book(testBook).isReading(true).isFavorite(false).build());

		// when & then
		mockMvc.perform(patch("/api/books/{userBookId}/favorite", userBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isFavorite").value(true));
	}

	@Test
	@DisplayName("GET /api/books/recent - 최근 읽은 책과 문장을 조회할 수 있다")
	void getRecentBook_returnsRecentBookWithQuotes() throws Exception {
		// given
		UserBook userBook = userBookRepository.save(UserBook.builder()
			.user(testUser).book(testBook).isReading(true).build());

		// when & then
		mockMvc.perform(get("/api/books/recent")
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.recentBook").exists())
			.andExpect(jsonPath("$.quotes").isArray());
	}

	@Test
	@DisplayName("DELETE /api/books/{userBookId} - 책을 삭제할 수 있다")
	void deleteBook_removesBookFromLibrary() throws Exception {
		// given
		UserBook userBook = userBookRepository.save(UserBook.builder()
			.user(testUser).book(testBook).isReading(true).build());

		// when & then
		mockMvc.perform(delete("/api/books/{userBookId}", userBook.getUserBookId())
				.header("Authorization", "Bearer " + accessToken))
			.andDo(print())
			.andExpect(status().isNoContent());
	}
}
