package com.example.seolab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteResponse {
	private UUID quoteId;
	private String text;
	private Integer page;
	private Boolean isFavorite;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
