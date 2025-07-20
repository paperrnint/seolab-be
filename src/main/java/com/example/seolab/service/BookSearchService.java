package com.example.seolab.service;

import com.example.seolab.dto.external.KakaoBookSearchResponse;
import com.example.seolab.dto.response.BookDto;
import com.example.seolab.dto.response.BookSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookSearchService {

	@Value("${kakao.api.key}")
	private String kakaoApiKey;

	@Value("${kakao.api.book-search-url}")
	private String bookSearchUrl;

	private final WebClient webClient;

	public BookSearchResponse searchBooks(String query) {
		return searchBooks(query, null, 1, 10);
	}

	public BookSearchResponse searchBooks(String query, String target) {
		return searchBooks(query, target, 1, 10);
	}

	public BookSearchResponse searchBooks(String query, String target, int page, int size) {
		try {
			log.info("Searching books with query: {}", query);

			KakaoBookSearchResponse kakaoResponse = webClient.get()
				.uri(uriBuilder -> {
					var builder = uriBuilder
						.path(bookSearchUrl.replace("https://dapi.kakao.com", ""))
						.queryParam("query", query)
						.queryParam("page", page)
						.queryParam("size", size)
						.queryParam("sort", "accuracy");

					// target이 지정된 경우에만 추가
					if (target != null && !target.isEmpty()) {
						builder.queryParam("target", target);
					}

					return builder.build();
				})
				.header("Authorization", "KakaoAK " + kakaoApiKey)
				.retrieve()
				.bodyToMono(KakaoBookSearchResponse.class)
				.block();

			if (kakaoResponse == null || kakaoResponse.getDocuments() == null) {
				log.warn("No response from Kakao API for query: {}", query);
				return BookSearchResponse.builder()
					.books(List.of())
					.totalCount(0)
					.isEnd(true)
					.build();
			}

			List<BookDto> books = kakaoResponse.getDocuments().stream()
				.map(this::convertToBookDto)
				.collect(Collectors.toList());

			log.info("Found {} books for query: {}", books.size(), query);

			return BookSearchResponse.builder()
				.books(books)
				.totalCount(kakaoResponse.getMeta().getTotalCount())
				.isEnd(kakaoResponse.getMeta().isEnd())
				.build();

		} catch (WebClientResponseException e) {
			log.error("Error calling Kakao API: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new RuntimeException("책 검색 중 오류가 발생했습니다.");
		} catch (Exception e) {
			log.error("Unexpected error during book search", e);
			throw new RuntimeException("책 검색 중 오류가 발생했습니다.");
		}
	}

	private BookDto convertToBookDto(KakaoBookSearchResponse.Document document) {
		return BookDto.builder()
			.title(HtmlUtils.htmlUnescape(document.getTitle()))
			.contents(HtmlUtils.htmlUnescape(document.getContents()))
			.isbn(document.getIsbn())
			.publishedDate(parseDate(document.getDatetime()))
			.authors(document.getAuthors())
			.publisher(document.getPublisher())
			.translators(document.getTranslators())
			.thumbnail(document.getThumbnail())
			.build();
	}

	private LocalDate parseDate(String datetime) {
		if (datetime == null || datetime.isEmpty()) {
			return null;
		}

		try {
			// ISO 8601 형식 파싱: 2014-11-17T00:00:00.000+09:00
			return LocalDate.parse(datetime.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
			log.warn("Failed to parse date: {}", datetime);
			return null;
		}
	}
}
