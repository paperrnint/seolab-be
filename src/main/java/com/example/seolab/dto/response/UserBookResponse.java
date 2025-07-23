package com.example.seolab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBookResponse {
	private Long userBookId;
	private BookInfo book;
	private LocalDate startDate;
	private LocalDate endDate;
	private Boolean isFavorite;
	private Boolean isReading;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class BookInfo {
		private Long bookId;
		private String title;
		private String contents;
		private String isbn;
		private LocalDate publishedDate;
		private List<String> authors;
		private String publisher;
		private List<String> translators;
		private String thumbnail;
	}
}
