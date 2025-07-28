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
public class UpdateQuoteRequest {

	@NotBlank(message = "문장 내용은 필수입니다.")
	@Size(max = 1000, message = "문장은 1000자를 초과할 수 없습니다.")
	private String text;

	private Integer page;
}
