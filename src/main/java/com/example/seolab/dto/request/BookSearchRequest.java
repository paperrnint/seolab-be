package com.example.seolab.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookSearchRequest {

	@NotBlank(message = "검색어는 필수입니다.")
	@Size(min = 1, max = 100, message = "검색어는 1-100자 사이여야 합니다.")
	private String query;
}
