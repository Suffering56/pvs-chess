package com.example.chess.exceptions;

public class GameNotFoundException extends ChessException {

	public GameNotFoundException() {
	}

	public GameNotFoundException(String message) {
		super(message);
	}

	public GameNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public GameNotFoundException(Throwable cause) {
		super(cause);
	}

	public GameNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
