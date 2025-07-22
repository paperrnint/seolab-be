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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_book_id")
	private Long userBookId;

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

	@Enumerated(EnumType.STRING)
	@Column(name = "reading_status")
	@Builder.Default
	private ReadingStatus readingStatus = ReadingStatus.READING;

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

	public enum ReadingStatus {
		READING("reading"),
		COMPLETED("completed");

		private final String value;

		ReadingStatus(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public LocalDate getStartDate() {
		return createdAt != null ? createdAt.toLocalDate() : null;
	}

	public void toggleReadingStatus() {
		if (this.readingStatus == ReadingStatus.READING) {
			// 읽는중 → 완료
			this.readingStatus = ReadingStatus.COMPLETED;
			this.endDate = LocalDate.now();
		} else {
			// 완료 → 읽는중
			this.readingStatus = ReadingStatus.READING;
			this.endDate = null; // 완독일 제거
		}
	}

	public void toggleFavorite() {
		this.isFavorite = !this.isFavorite;
	}

	public boolean isReading() {
		return this.readingStatus == ReadingStatus.READING;
	}

	public boolean isCompleted() {
		return this.readingStatus == ReadingStatus.COMPLETED;
	}
}
