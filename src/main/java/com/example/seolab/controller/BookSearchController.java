package com.example.seolab.controller;

import com.example.seolab.dto.response.BookSearchResponse;
import com.example.seolab.service.BookSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Validated
public class BookSearchController {

	private final BookSearchService bookSearchService;

	@GetMapping("/search")
	public ResponseEntity<BookSearchResponse> searchBooks(
		@RequestParam
		@NotBlank(message = "검색어는 필수입니다.")
		@Size(min = 1, max = 100, message = "검색어는 1-100자 사이여야 합니다.")
		String query,

		@RequestParam(required = false)
		String target,

		@RequestParam(defaultValue = "1")
		int page,

		@RequestParam(defaultValue = "10")
		int size) {

		BookSearchResponse response = bookSearchService.searchBooks(query, target, page, size);
		return ResponseEntity.ok(response);
	}
}
