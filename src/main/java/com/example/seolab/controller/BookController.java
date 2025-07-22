package com.example.seolab.controller;

import com.example.seolab.dto.request.AddBookRequest;
import com.example.seolab.dto.response.AddBookResponse;
import com.example.seolab.entity.User;
import com.example.seolab.entity.UserBook;
import com.example.seolab.service.UserBookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

	private final UserBookService userBookService;

	@PostMapping("/add")
	public ResponseEntity<AddBookResponse> addBookToLibrary(
		@Valid @RequestBody AddBookRequest request,
		Authentication authentication) {

		String userEmail = authentication.getName();
		log.info("User {} is adding book: {}", userEmail, request.getBookInfo().getTitle());

		// Authentication에서 실제 사용자 ID 추출
		Long userId = getUserIdFromAuthentication(authentication);

		AddBookResponse response = userBookService.addBookToUserLibrary(userId, request);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/reading")
	public ResponseEntity<List<UserBook>> getReadingBooks(Authentication authentication) {
		Long userId = getUserIdFromAuthentication(authentication);
		List<UserBook> readingBooks = userBookService.getReadingBooks(userId);

		return ResponseEntity.ok(readingBooks);
	}

	@GetMapping("/favorites")
	public ResponseEntity<List<UserBook>> getFavoriteBooks(Authentication authentication) {
		Long userId = getUserIdFromAuthentication(authentication);
		List<UserBook> favoriteBooks = userBookService.getFavoriteBooks(userId);

		return ResponseEntity.ok(favoriteBooks);
	}

	@PatchMapping("/{userBookId}/complete")
	public ResponseEntity<UserBook> markBookAsCompleted(
		@PathVariable Long userBookId,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		UserBook completedBook = userBookService.markBookAsCompleted(userId, userBookId);

		return ResponseEntity.ok(completedBook);
	}

	@PatchMapping("/{userBookId}/favorite")
	public ResponseEntity<UserBook> toggleFavorite(
		@PathVariable Long userBookId,
		Authentication authentication) {

		Long userId = getUserIdFromAuthentication(authentication);
		UserBook toggledBook = userBookService.toggleFavorite(userId, userBookId);

		return ResponseEntity.ok(toggledBook);
	}

	private Long getUserIdFromAuthentication(Authentication authentication) {
		// Spring Security에서 인증된 User 객체 가져오기
		User user = (User) authentication.getPrincipal();
		return user.getUserId();
	}
}
