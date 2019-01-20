package com.example.chess.logic.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class CustomResponse extends ResponseEntity<String> {

	private CustomResponse(String body, HttpStatus status) {
		super(body, status);
	}

	public static CustomResponse internalServerError(String body) {
		return new CustomResponse(body, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * fix the following error => XML Parsing Error: no root element found
	 */
	public static CustomResponse createVoid() {
		return new CustomResponse("", HttpStatus.OK);
	}

}
