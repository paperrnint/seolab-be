package com.example.seolab.service;

import com.example.seolab.dto.request.AddQuoteRequest;
import com.example.seolab.dto.request.UpdateQuoteRequest;
import com.example.seolab.dto.response.QuoteResponse;
import com.example.seolab.entity.Quote;
import com.example.seolab.entity.UserBook;
import com.example.seolab.exception.AccessDeniedException;
import com.example.seolab.repository.QuoteRepository;
import com.example.seolab.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QuoteService {

	private final QuoteRepository quoteRepository;
	private final UserBookRepository userBookRepository;

	public QuoteResponse addQuote(Long userId, UUID userBookId, AddQuoteRequest request) {
		log.info("Adding quote to userBook {} for user {}", userBookId, userId);

		UserBook userBook = findUserBookByIdAndUserId(userBookId, userId);

		Quote quote = Quote.builder()
			.userBook(userBook)
			.text(request.getText().trim())
			.page(request.getPage())
			.isFavorite(false)
			.build();

		Quote savedQuote = quoteRepository.save(quote);

		userBook.updateLastActivity();
		userBookRepository.save(userBook);

		log.info("Successfully added quote with ID: {}", savedQuote.getQuoteId());

		return convertToQuoteResponse(savedQuote);
	}

	@Transactional(readOnly = true)
	public List<QuoteResponse> getQuotesByUserBook(Long userId, UUID userBookId, Boolean favorite) {
		// 사용자 권한 체크
		findUserBookByIdAndUserId(userBookId, userId);

		List<Quote> quotes;
		if (favorite != null && favorite) {
			quotes = quoteRepository.findByUserBookUserBookIdAndIsFavoriteTrueOrderByCreatedAtAsc(userBookId);
		} else {
			quotes = quoteRepository.findByUserBookUserBookIdOrderByCreatedAtAsc(userBookId);
		}

		return quotes.stream()
			.map(this::convertToQuoteResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<QuoteResponse> getAllUserQuotes(Long userId, Boolean favorite) {
		List<Quote> quotes;
		if (favorite != null && favorite) {
			quotes = quoteRepository.findFavoriteQuotesByUserId(userId);
		} else {
			quotes = quoteRepository.findByUserIdOrderByCreatedAtAsc(userId);
		}

		return quotes.stream()
			.map(this::convertToQuoteResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public QuoteResponse getQuote(Long userId, UUID quoteId) {
		Quote quote = findQuoteByIdAndUserId(quoteId, userId);
		return convertToQuoteResponse(quote);
	}

	public QuoteResponse updateQuote(Long userId, UUID quoteId, UpdateQuoteRequest request) {
		Quote quote = findQuoteByIdAndUserId(quoteId, userId);

		quote.setText(request.getText().trim());
		quote.setPage(request.getPage());

		Quote savedQuote = quoteRepository.save(quote);

		quote.getUserBook().updateLastActivity();
		userBookRepository.save(quote.getUserBook());

		log.info("Updated quote with ID: {}", quoteId);

		return convertToQuoteResponse(savedQuote);
	}

	public QuoteResponse toggleQuoteFavorite(Long userId, UUID quoteId) {
		Quote quote = findQuoteByIdAndUserId(quoteId, userId);
		quote.toggleFavorite();

		Quote savedQuote = quoteRepository.save(quote);
		log.info("Toggled favorite for quote {} ({})", quoteId, quote.getIsFavorite());

		return convertToQuoteResponse(savedQuote);
	}

	public void deleteQuote(Long userId, UUID quoteId) {
		Quote quote = findQuoteByIdAndUserId(quoteId, userId);
		UserBook userBook = quote.getUserBook();

		quoteRepository.delete(quote);

		userBook.updateLastActivity();
		userBookRepository.save(userBook);

		log.info("Deleted quote with ID: {}", quoteId);
	}


	@Transactional(readOnly = true)
	public QuoteResponse getQuoteByUserBookAndQuoteId(Long userId, UUID userBookId, UUID quoteId) {
		// userBook 권한 체크
		findUserBookByIdAndUserId(userBookId, userId);

		Quote quote = findQuoteByIdAndUserId(quoteId, userId);

		// quote가 해당 userBook에 속하는지 확인
		if (!quote.getUserBook().getUserBookId().equals(userBookId)) {
			throw new IllegalArgumentException("해당 책에 속하지 않는 문장입니다.");
		}

		return convertToQuoteResponse(quote);
	}

	public QuoteResponse updateQuoteByUserBook(Long userId, UUID userBookId, UUID quoteId, UpdateQuoteRequest request) {
		// userBook 권한 체크
		UserBook userBook = findUserBookByIdAndUserId(userBookId, userId);

		Quote quote = findQuoteByIdAndUserId(quoteId, userId);

		// quote가 해당 userBook에 속하는지 확인
		if (!quote.getUserBook().getUserBookId().equals(userBookId)) {
			throw new IllegalArgumentException("해당 책에 속하지 않는 문장입니다.");
		}

		quote.setText(request.getText().trim());
		quote.setPage(request.getPage());

		Quote savedQuote = quoteRepository.save(quote);

		userBook.updateLastActivity();
		userBookRepository.save(userBook);

		log.info("Updated quote with ID: {} in userBook: {}", quoteId, userBookId);

		return convertToQuoteResponse(savedQuote);
	}

	public QuoteResponse toggleQuoteFavoriteByUserBook(Long userId, UUID userBookId, UUID quoteId) {
		// userBook 권한 체크
		findUserBookByIdAndUserId(userBookId, userId);

		Quote quote = findQuoteByIdAndUserId(quoteId, userId);

		// quote가 해당 userBook에 속하는지 확인
		if (!quote.getUserBook().getUserBookId().equals(userBookId)) {
			throw new IllegalArgumentException("해당 책에 속하지 않는 문장입니다.");
		}

		quote.toggleFavorite();

		Quote savedQuote = quoteRepository.save(quote);
		log.info("Toggled favorite for quote {} in userBook {} ({})", quoteId, userBookId, quote.getIsFavorite());

		return convertToQuoteResponse(savedQuote);
	}

	public void deleteQuoteByUserBook(Long userId, UUID userBookId, UUID quoteId) {
		// userBook 권한 체크
		UserBook userBook = findUserBookByIdAndUserId(userBookId, userId);

		Quote quote = findQuoteByIdAndUserId(quoteId, userId);

		// quote가 해당 userBook에 속하는지 확인
		if (!quote.getUserBook().getUserBookId().equals(userBookId)) {
			throw new IllegalArgumentException("해당 책에 속하지 않는 문장입니다.");
		}

		quoteRepository.delete(quote);

		userBook.updateLastActivity();
		userBookRepository.save(userBook);

		log.info("Deleted quote with ID: {} from userBook: {}", quoteId, userBookId);
	}

	@Transactional(readOnly = true)
	public List<QuoteResponse> getRecentQuotes(Long userId, int limit) {
		List<Quote> quotes = quoteRepository.findRecentQuotesByUserId(userId);
		return quotes.stream()
			.limit(limit)
			.map(this::convertToQuoteResponse)
			.toList();
	}

	private UserBook findUserBookByIdAndUserId(UUID userBookId, Long userId) {
		return userBookRepository.findById(userBookId)
			.filter(ub -> ub.getUser().getUserId().equals(userId))
			.orElseThrow(() -> new AccessDeniedException("접근할 수 없는 책입니다."));
	}

	private Quote findQuoteByIdAndUserId(UUID quoteId, Long userId) {
		return quoteRepository.findByQuoteIdAndUserId(quoteId, userId)
			.orElseThrow(() -> new AccessDeniedException("접근할 수 없는 문장입니다."));
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
}
