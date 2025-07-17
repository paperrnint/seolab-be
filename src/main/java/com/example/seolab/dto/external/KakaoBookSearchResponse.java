package com.example.seolab.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoBookSearchResponse {
	private Meta meta;
	private List<Document> documents;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Meta {
		@JsonProperty("total_count")
		private int totalCount;

		@JsonProperty("pageable_count")
		private int pageableCount;

		@JsonProperty("is_end")
		private boolean isEnd;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Document {
		private String title;
		private String contents;
		private String url;
		private String isbn;
		private String datetime;
		private List<String> authors;
		private String publisher;
		private List<String> translators;
		private Integer price;

		@JsonProperty("sale_price")
		private Integer salePrice;

		private String thumbnail;
		private String status;
	}
}
