package com.example.seolab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookDto {
	private String title;
	private String contents;
	private String isbn;
	private LocalDate publishedDate;
	private List<String> authors;
	private String publisher;
	private List<String> translators;
	private String thumbnail;
}
