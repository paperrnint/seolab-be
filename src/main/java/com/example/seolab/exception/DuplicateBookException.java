package com.example.seolab.exception;

import java.util.UUID;

public class DuplicateBookException extends RuntimeException {
	private final UUID userBookId;

	public DuplicateBookException(String message, UUID userBookId) {
		super(message);
		this.userBookId = userBookId;
	}

	public UUID getUserBookId() {
		return userBookId;
	}
}
