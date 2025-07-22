package com.example.seolab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
	name = "books",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "unique_book",
			columnNames = {"title", "author", "publisher"}
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

	@Column(nullable = false)
	private String author;

	private String publisher;

	private String isbn;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(name = "cover_image")
	private String coverImage;

	@Column(name = "published_date")
	private LocalDate publishedDate;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
