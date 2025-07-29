package com.example.seolab.controller;

import com.example.seolab.dto.request.AddQuoteRequest;
import com.example.seolab.dto.request.UpdateQuoteRequest;
import com.example.seolab.dto.response.QuoteResponse;
import com.example.seolab.entity.User;
import com.example.seolab.service.QuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/books/{userBookId}/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

	private final QuoteService quoteService;

	// POST /api/books/{userBookId}/quotes - 문장 추가
	@PostMapping
	public ResponseEntity<QuoteResponse> addQuote(
		@PathVariable UUID userBookId,
		@Valid @RequestBody AddQuoteRequest request,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		QuoteResponse response = quoteService.addQuote(userId, userBookId, request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// GET /api/books/{userBookId}/quotes - 해당 책의 문장 목록
	@GetMapping
	public ResponseEntity<List<QuoteResponse>> getQuotesByUserBook(
		@PathVariable UUID userBookId,
		@RequestParam(required = false) Boolean favorite,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		List<QuoteResponse> quotes = quoteService.getQuotesByUserBook(userId, userBookId, favorite);

		return ResponseEntity.ok(quotes);
	}

	// GET /api/books/{userBookId}/quotes/{quoteId} - 특정 문장 조회
	@GetMapping("/{quoteId}")
	public ResponseEntity<QuoteResponse> getQuote(
		@PathVariable UUID userBookId,
		@PathVariable UUID quoteId,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		QuoteResponse quote = quoteService.getQuoteByUserBookAndQuoteId(userId, userBookId, quoteId);

		return ResponseEntity.ok(quote);
	}

	// PUT /api/books/{userBookId}/quotes/{quoteId} - 문장 수정
	@PutMapping("/{quoteId}")
	public ResponseEntity<QuoteResponse> updateQuote(
		@PathVariable UUID userBookId,
		@PathVariable UUID quoteId,
		@Valid @RequestBody UpdateQuoteRequest request,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		QuoteResponse response = quoteService.updateQuoteByUserBook(userId, userBookId, quoteId, request);

		return ResponseEntity.ok(response);
	}

	// DELETE /api/books/{userBookId}/quotes/{quoteId} - 문장 삭제
	@DeleteMapping("/{quoteId}")
	public ResponseEntity<Void> deleteQuote(
		@PathVariable UUID userBookId,
		@PathVariable UUID quoteId,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		quoteService.deleteQuoteByUserBook(userId, userBookId, quoteId);

		return ResponseEntity.noContent().build();
	}

	// PATCH /api/books/{userBookId}/quotes/{quoteId}/favorite - 즐겨찾기
	@PatchMapping("/{quoteId}/favorite")
	public ResponseEntity<QuoteResponse> toggleQuoteFavorite(
		@PathVariable UUID userBookId,
		@PathVariable UUID quoteId,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		QuoteResponse response = quoteService.toggleQuoteFavoriteByUserBook(userId, userBookId, quoteId);

		return ResponseEntity.ok(response);
	}

	private Long getUserIdFromAuthentication(Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		return user.getUserId();
	}
}
