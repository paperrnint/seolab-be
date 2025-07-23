package com.example.seolab.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddBookRequest {

	@NotNull(message = "책 제목은 필수입니다.")
	private String title;

	private String contents;
	private String isbn;
	private String publishedDate;
	private List<String> authors;
	private String publisher;
	private List<String> translators;
	private String thumbnail;
}
