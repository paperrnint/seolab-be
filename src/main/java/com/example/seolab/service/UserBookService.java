package com.example.seolab.service;

import com.example.seolab.dto.request.AddBookRequest;
import com.example.seolab.dto.response.AddBookResponse;
import com.example.seolab.dto.response.BookDto;
import com.example.seolab.dto.response.QuoteResponse;
import com.example.seolab.dto.response.RecentBookResponse;
import com.example.seolab.dto.response.UserBookResponse;
import com.example.seolab.entity.Book;
import com.example.seolab.entity.Quote;
import com.example.seolab.entity.User;
import com.example.seolab.entity.UserBook;
import com.example.seolab.exception.AccessDeniedException;
import com.example.seolab.exception.DuplicateBookException;
import com.example.seolab.repository.QuoteRepository;
import com.example.seolab.repository.UserBookRepository;
import com.example.seolab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserBookService {

	private final UserBookRepository userBookRepository;
	private final UserRepository userRepository;
	private final BookService bookService;
	private final QuoteRepository quoteRepository;

	public AddBookResponse addBookToUserLibrary(Long userId, AddBookRequest request) {
		log.info("Adding book to user {} library: {}", userId, request.getTitle());

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

		BookDto bookDto = convertToBookDto(request);

		Book book = bookService.findOrCreateBook(bookDto);

		Optional<UserBook> existingUserBook =
			userBookRepository.findByUserUserIdAndBookBookId(userId, book.getBookId());

		if (existingUserBook.isPresent()) {
			UserBook userBook = existingUserBook.get();
			UUID userBookId = userBook.getUserBookId();
			throw new DuplicateBookException("이미 내 서재에 추가된 책입니다.", userBookId);
		}

		UserBook userBook = UserBook.builder()
			.user(user)
			.book(book)
			.isReading(true)
			.isFavorite(false)
			.build();

		UserBook savedUserBook = userBookRepository.save(userBook);
		log.info("Successfully added book to user library. UserBook ID: {}", savedUserBook.getUserBookId());

		return AddBookResponse.builder()
			.userBookId(savedUserBook.getUserBookId())
			.message("책이 성공적으로 추가되었습니다.")
			.build();
	}

	@Transactional(readOnly = true)
	public List<UserBookResponse> getUserBooks(Long userId, Boolean favorite, Boolean reading) {
		List<UserBook> userBooks;

		// 쿼리 파라미터에 따른 조건부 조회 - 모두 updatedAt 기준으로 변경
		if (favorite != null && favorite && reading != null) {
			// favorite=true&reading=true/false: 즐겨찾기이면서 읽는 중/완독인 책
			userBooks = userBookRepository.findByUserUserIdAndIsFavoriteTrueAndIsReadingOrderByUpdatedAtDesc(
				userId, reading);
		} else if (favorite != null && favorite) {
			// favorite=true: 즐겨찾기 책만
			userBooks = userBookRepository.findByUserUserIdAndIsFavoriteTrueOrderByUpdatedAtDesc(userId);
		} else if (reading != null) {
			// reading=true/false: 읽는 중/완독인 책만
			userBooks = userBookRepository.findByUserUserIdAndIsReadingOrderByUpdatedAtDesc(userId, reading);
		} else {
			// 파라미터 없음: 전체 책 목록
			userBooks = userBookRepository.findByUserUserIdOrderByUpdatedAtDesc(userId);
		}

		return userBooks.stream()
			.map(this::convertToUserBookResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public UserBookResponse getUserBook(Long userId, UUID userBookId) {
		UserBook userBook = findUserBookByIdAndUserId(userBookId, userId);
		return convertToUserBookResponse(userBook);
	}

	@Transactional(readOnly = true)
	public RecentBookResponse getRecentBookWithQuotes(Long userId) {
		log.info("Getting recent book with quotes for user: {}", userId);

		// updated_at 기준으로 가장 최근 책 조회
		Optional<UserBook> recentUserBookOpt = userBookRepository.findTopByUserUserIdOrderByUpdatedAtDesc(userId);

		if (recentUserBookOpt.isEmpty()) {
			log.info("No books found for user: {}", userId);
			return RecentBookResponse.builder()
				.recentBook(null)
				.quotes(List.of())
				.build();
		}

		UserBook recentUserBook = recentUserBookOpt.get();
		UserBookResponse recentBookResponse = convertToUserBookResponse(recentUserBook);

		// 해당 책의 최근 문장 4개 조회 (created_at 기준 최신순)
		List<Quote> recentQuotes = quoteRepository.findByUserBookUserBookIdOrderByCreatedAtDesc(
				recentUserBook.getUserBookId())
			.stream()
			.limit(4)
			.toList();

		List<QuoteResponse> quoteResponses = recentQuotes.stream()
			.map(this::convertToQuoteResponse)
			.toList();

		log.info("Found recent book '{}' with {} quotes for user: {}",
			recentUserBook.getBook().getTitle(), quoteResponses.size(), userId);

		return RecentBookResponse.builder()
			.recentBook(recentBookResponse)
			.quotes(quoteResponses)
			.build();
	}

	private QuoteResponse convertToQuoteResponse(Quote quote) {
		return QuoteResponse.builder()
			.quoteId(quote.getQuoteId())
			.text(quote.getText())
			.page(quote.getPage())
			.isFavorite(quote.getIsFavorite())
			.createdAt(quote.getCreatedAt())
			.updatedAt(quote.getUpdatedAt())
			.build();
	}

	public UserBookResponse markBookAsCompleted(Long userId, UUID userBookId) {
		UserBook userBook = findUserBookByIdAndUserId(userBookId, userId);

		boolean beforeReading = userBook.getIsReading();
		userBook.toggleReading(); // 읽는 중 ↔ 완독 토글
		boolean afterReading = userBook.getIsReading();

		log.info("User {} toggled reading status for book: {} ({} → {})",
			userId, userBook.getBook().getTitle(),
			beforeReading ? "reading" : "completed",
			afterReading ? "reading" : "completed");

		UserBook savedUserBook = userBookRepository.save(userBook);
		return convertToUserBookResponse(savedUserBook);
	}

	public UserBookResponse toggleFavorite(Long userId, UUID userBookId) {
		UserBook userBook = findUserBookByIdAndUserId(userBookId, userId);
		userBook.toggleFavorite();

		log.info("User {} toggled favorite for book: {} ({})",
			userId, userBook.getBook().getTitle(), userBook.getIsFavorite());

		UserBook savedUserBook = userBookRepository.save(userBook);
		return convertToUserBookResponse(savedUserBook);
	}

	private UserBook findUserBookByIdAndUserId(UUID userBookId, Long userId) {
		return userBookRepository.findById(userBookId)
			.filter(ub -> ub.getUser().getUserId().equals(userId))
			.orElseThrow(() -> new AccessDeniedException("접근할 수 없는 책입니다."));
	}

	private BookDto convertToBookDto(AddBookRequest request) {
		LocalDate publishedDate = parsePublishedDate(request.getPublishedDate());

		return BookDto.builder()
			.title(request.getTitle())
			.contents(request.getContents())
			.isbn(request.getIsbn())
			.publishedDate(publishedDate)
			.authors(request.getAuthors())
			.publisher(request.getPublisher())
			.translators(request.getTranslators())
			.thumbnail(request.getThumbnail())
			.build();
	}

	private LocalDate parsePublishedDate(String dateString) {
		if (dateString == null || dateString.isEmpty()) {
			return null;
		}

		try {
			if (dateString.contains("T")) {
				dateString = dateString.substring(0, 10);
			}
			return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException e) {
			log.warn("Failed to parse published date: {}", dateString);
			return null;
		}
	}

	private UserBookResponse convertToUserBookResponse(UserBook userBook) {
		Book book = userBook.getBook();

		UserBookResponse.BookInfo bookInfo = UserBookResponse.BookInfo.builder()
			.title(book.getTitle())
			.isbn(book.getIsbn())
			.publishedDate(book.getPublishedDate())
			.authors(book.getAuthors())
			.publisher(book.getPublisher())
			.translators(book.getTranslators())
			.thumbnail(book.getThumbnail())
			.build();

		long quoteCount = quoteRepository.countByUserBookUserBookId(userBook.getUserBookId());

		return UserBookResponse.builder()
			.userBookId(userBook.getUserBookId())
			.book(bookInfo)
			.startDate(userBook.getStartDate())
			.endDate(userBook.getEndDate())
			.isFavorite(userBook.getIsFavorite())
			.isReading(userBook.getIsReading())
			.createdAt(userBook.getCreatedAt())
			.updatedAt(userBook.getUpdatedAt())
			.quoteCount(quoteCount)
			.build();
	}
}
