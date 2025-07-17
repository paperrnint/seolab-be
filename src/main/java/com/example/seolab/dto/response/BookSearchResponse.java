package com.example.seolab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookSearchResponse {
	private List<BookDto> books;
	private int totalCount;

	@JsonProperty("isEnd")
	private boolean isEnd;
}
