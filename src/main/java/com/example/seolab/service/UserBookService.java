package com.example.seolab.service;

import com.example.seolab.dto.request.AddBookRequest;
import com.example.seolab.dto.response.AddBookResponse;
import com.example.seolab.dto.response.BookDto;
import com.example.seolab.dto.response.UserBookResponse;
import com.example.seolab.entity.Book;
import com.example.seolab.entity.User;
import com.example.seolab.entity.UserBook;
import com.example.seolab.exception.DuplicateBookException;
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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserBookService {

	private final UserBookRepository userBookRepository;
	private final UserRepository userRepository;
	private final BookService bookService;

	public AddBookResponse addBookToUserLibrary(Long userId, AddBookRequest request) {
		log.info("Adding book to user {} library: {}", userId, request.getBookInfo().getTitle());

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

		BookDto bookDto = convertToBookDto(request.getBookInfo());

		Book book = bookService.findOrCreateBook(bookDto);

		Optional<UserBook> existingUserBook =
			userBookRepository.findByUserUserIdAndBookBookId(userId, book.getBookId());

		if (existingUserBook.isPresent()) {
			throw new DuplicateBookException("이미 내 서재에 추가된 책입니다.");
		}

		UserBook userBook = UserBook.builder()
			.user(user)
			.book(book)
			.readingStatus(UserBook.ReadingStatus.READING)
			.isFavorite(false)
			.build();

		UserBook savedUserBook = userBookRepository.save(userBook);
		log.info("Successfully added book to user library. UserBook ID: {}", savedUserBook.getUserBookId());

		return buildAddBookResponse(savedUserBook);
	}

	@Transactional(readOnly = true)
	public List<UserBookResponse> getReadingBooks(Long userId) {
		List<UserBook> userBooks = userBookRepository.findByUserUserIdAndReadingStatusOrderByCreatedAtDesc(
			userId, UserBook.ReadingStatus.READING);
		return userBooks.stream()
			.map(this::convertToUserBookResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<UserBookResponse> getFavoriteBooks(Long userId) {
		List<UserBook> userBooks = userBookRepository.findByUserUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(userId);
		return userBooks.stream()
			.map(this::convertToUserBookResponse)
			.toList();
	}

	public UserBookResponse markBookAsCompleted(Long userId, Long userBookId) {
		UserBook userBook = findUserBookByIdAndUserId(userBookId, userId);

		String beforeStatus = userBook.getReadingStatus().getValue();
		userBook.toggleReadingStatus(); // 기존 완료 처리를 토글로 변경
		String afterStatus = userBook.getReadingStatus().getValue();

		log.info("User {} toggled reading status for book: {} ({} → {})",
			userId, userBook.getBook().getTitle(), beforeStatus, afterStatus);

		UserBook savedUserBook = userBookRepository.save(userBook);
		return convertToUserBookResponse(savedUserBook);
	}

	public UserBookResponse toggleFavorite(Long userId, Long userBookId) {
		UserBook userBook = findUserBookByIdAndUserId(userBookId, userId);
		userBook.toggleFavorite();

		log.info("User {} toggled favorite for book: {} ({})",
			userId, userBook.getBook().getTitle(), userBook.getIsFavorite());
		UserBook savedUserBook = userBookRepository.save(userBook);
		return convertToUserBookResponse(savedUserBook);
	}

	private UserBook findUserBookByIdAndUserId(Long userBookId, Long userId) {
		return userBookRepository.findById(userBookId)
			.filter(ub -> ub.getUser().getUserId().equals(userId))
			.orElseThrow(() -> new IllegalArgumentException("접근할 수 없는 책입니다."));
	}

	private BookDto convertToBookDto(AddBookRequest.BookInfo bookInfo) {
		LocalDate publishedDate = parsePublishedDate(bookInfo.getPublishedDate());

		return BookDto.builder()
			.title(bookInfo.getTitle())
			.contents(bookInfo.getContents())
			.isbn(bookInfo.getIsbn())
			.publishedDate(publishedDate)
			.authors(bookInfo.getAuthors())
			.publisher(bookInfo.getPublisher())
			.translators(bookInfo.getTranslators())
			.thumbnail(bookInfo.getThumbnail())
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

	private AddBookResponse buildAddBookResponse(UserBook userBook) {
		Book book = userBook.getBook();

		AddBookResponse.BookDetail bookDetail = AddBookResponse.BookDetail.builder()
			.bookId(book.getBookId())
			.title(book.getTitle())
			.author(book.getAuthor())
			.publisher(book.getPublisher())
			.isbn(book.getIsbn())
			.description(book.getDescription())
			.coverImage(book.getCoverImage())
			.publishedDate(book.getPublishedDate())
			.build();

		return AddBookResponse.builder()
			.userBookId(userBook.getUserBookId())
			.book(bookDetail)
			.startDate(userBook.getStartDate())
			.readingStatus(userBook.getReadingStatus().getValue())
			.isFavorite(userBook.getIsFavorite())
			.createdAt(userBook.getCreatedAt())
			.message("책이 성공적으로 추가되었습니다.")
			.build();
	}

	private UserBookResponse convertToUserBookResponse(UserBook userBook) {
		Book book = userBook.getBook();

		UserBookResponse.BookInfo bookInfo = UserBookResponse.BookInfo.builder()
			.bookId(book.getBookId())
			.title(book.getTitle())
			.author(book.getAuthor())
			.publisher(book.getPublisher())
			.isbn(book.getIsbn())
			.description(book.getDescription())
			.coverImage(book.getCoverImage())
			.publishedDate(book.getPublishedDate())
			.build();

		return UserBookResponse.builder()
			.userBookId(userBook.getUserBookId())
			.book(bookInfo)
			.startDate(userBook.getStartDate())
			.endDate(userBook.getEndDate())
			.isFavorite(userBook.getIsFavorite())
			.readingStatus(userBook.getReadingStatus().getValue())
			.createdAt(userBook.getCreatedAt())
			.updatedAt(userBook.getUpdatedAt())
			.build();
	}
}
