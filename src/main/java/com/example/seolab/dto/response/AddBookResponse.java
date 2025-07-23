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
public class AddBookResponse {

	private Long userBookId;
	private BookDetail book;
	private LocalDate startDate;
	private Boolean isFavorite;
	private Boolean isReading;
	private LocalDateTime createdAt;
	private String message;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class BookDetail {
		private Long bookId;
		private String title;
		private String author;
		private String publisher;
		private String isbn;
		private String description;
		private String coverImage;
		private LocalDate publishedDate;
	}
}
