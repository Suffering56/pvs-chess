package com.example.chess.exceptions;

public class NavigationException extends Exception {

	public NavigationException() {
	}

	public NavigationException(String message) {
		super(message);
	}

	public NavigationException(String message, Throwable cause) {
		super(message, cause);
	}

	public NavigationException(Throwable cause) {
		super(cause);
	}

	public NavigationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
