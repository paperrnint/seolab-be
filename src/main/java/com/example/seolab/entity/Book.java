package com.example.seolab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
	name = "books",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "unique_book",
			columnNames = {"title", "authors", "publisher"}
		)
	}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "book_id")
	private Long bookId;

	@Column(nullable = false)
	private String title;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "JSON")
	private List<String> authors;  // JSON 배열로 저장

	private String publisher;

	private String isbn;

	@Column(columnDefinition = "TEXT")
	private String contents;

	private String thumbnail;

	@Column(name = "published_date")
	private LocalDate publishedDate;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "JSON")
	private List<String> translators;  // JSON 배열로 저장

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}

	// 편의 메소드: 첫 번째 저자 반환 (기존 로직 호환용)
	public String getFirstAuthor() {
		return authors != null && !authors.isEmpty() ? authors.get(0) : "";
	}
}
