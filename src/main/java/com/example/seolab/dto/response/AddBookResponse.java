package com.example.seolab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddBookResponse {

	private UUID userBookId;
	private BookDetail book;
	private LocalDate startDate;
	private Boolean isReading;
	private Boolean isFavorite;
	private LocalDateTime createdAt;
	private String message;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class BookDetail {
		private String title;
		private String isbn;
		private LocalDate publishedDate;
		private List<String> authors;
		private String publisher;
		private List<String> translators;
		private String thumbnail;
	}
}
