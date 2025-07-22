package com.example.seolab.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddBookRequest {
	@NotNull(message = "책 정보는 필수입니다.")
	@Valid
	private BookInfo bookInfo;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class BookInfo {

		@NotNull(message = "책 제목은 필수입니다.")
		private String title;

		private String contents;
		private String isbn;
		private String publishedDate; // ISO 형태 문자열
		private java.util.List<String> authors;
		private String publisher;
		private java.util.List<String> translators;
		private String thumbnail;
	}
}
