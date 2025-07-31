package com.example.seolab.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
	name = "user_books",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "unique_user_book",
			columnNames = {"user_id", "book_id"}
		)
	}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBook {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "user_book_id", columnDefinition = "BINARY(16)")
	private UUID userBookId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "is_favorite")
	@Builder.Default
	private Boolean isFavorite = false;

	@Column(name = "is_reading")
	@Builder.Default
	private Boolean isReading = true;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}


	public LocalDate getStartDate() {
		return createdAt != null ? createdAt.toLocalDate() : null;
	}

	public void toggleReading() {
		if (this.isReading) {
			this.isReading = false;
			this.endDate = LocalDate.now();
		} else {
			this.isReading = true;
			this.endDate = null; // 완독일 제거
		}
	}

	public void toggleFavorite() {
		this.isFavorite = !this.isFavorite;
	}

	// 문장 활동시에만 호출되는 메서드
	public void updateLastActivity() {
		this.updatedAt = LocalDateTime.now();
	}

	public boolean isCurrentlyReading() {
		return this.isReading;
	}

	public boolean isCompleted() {
		return !this.isReading;
	}
}
