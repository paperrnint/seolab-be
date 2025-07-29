package com.example.seolab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "quote_id", columnDefinition = "BINARY(16)")
	private UUID quoteId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_book_id", nullable = false)
	private UserBook userBook;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String text;

	@Column(name = "page")
	private Integer page;

	@Column(name = "is_favorite")
	@Builder.Default
	private Boolean isFavorite = false;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public void toggleFavorite() {
		this.isFavorite = !this.isFavorite;
	}
}
