package com.example.seolab.controller;

import com.example.seolab.dto.request.AddBookRequest;
import com.example.seolab.dto.response.AddBookResponse;
import com.example.seolab.dto.response.RecentBookResponse;
import com.example.seolab.dto.response.UserBookResponse;
import com.example.seolab.entity.User;
import com.example.seolab.service.UserBookService;
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
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

	private final UserBookService userBookService;

	@PostMapping
	public ResponseEntity<AddBookResponse> addBookToLibrary(
		@Valid @RequestBody AddBookRequest request,
		Authentication authentication) {

		String userEmail = authentication.getName();
		log.info("User {} is adding book: {}", userEmail, request.getTitle());

		// Authentication에서 실제 사용자 ID 추출
		Long userId = getUserIdFromAuthentication(authentication);

		AddBookResponse response = userBookService.addBookToUserLibrary(userId, request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<UserBookResponse>> getUserBooks(
		@RequestParam(required = false) Boolean favorite,
		@RequestParam(required = false) Boolean reading,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		List<UserBookResponse> books = userBookService.getUserBooks(userId, favorite, reading);

		return ResponseEntity.ok(books);
	}

	@PatchMapping("/{userBookId}/complete")
	public ResponseEntity<UserBookResponse> markBookAsCompleted(
		@PathVariable UUID userBookId,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		UserBookResponse toggledBook = userBookService.markBookAsCompleted(userId, userBookId);

		return ResponseEntity.ok(toggledBook);
	}

	@PatchMapping("/{userBookId}/favorite")
	public ResponseEntity<UserBookResponse> toggleFavorite(
		@PathVariable UUID userBookId,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		UserBookResponse toggledBook = userBookService.toggleFavorite(userId, userBookId);

		return ResponseEntity.ok(toggledBook);
	}

	@GetMapping("/{userBookId}")
	public ResponseEntity<UserBookResponse> getUserBook(
		@PathVariable UUID userBookId,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		UserBookResponse userBook = userBookService.getUserBook(userId, userBookId);

		return ResponseEntity.ok(userBook);
	}

	@GetMapping("/recent")
	public ResponseEntity<RecentBookResponse> getRecentBook(Authentication authentication) {
		Long userId = getUserIdFromAuthentication(authentication);
		RecentBookResponse response = userBookService.getRecentBookWithQuotes(userId);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{userBookId}")
	public ResponseEntity<Void> deleteBook(
		@PathVariable UUID userBookId,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		userBookService.deleteUserBook(userId, userBookId);

		return ResponseEntity.noContent().build();
	}

	private Long getUserIdFromAuthentication(Authentication authentication) {
		User user = (User) authentication.getPrincipal();
		return user.getUserId();
	}
}
