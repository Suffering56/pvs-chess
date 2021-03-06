package com.example.chess.dto.exceptions;

import com.example.chess.logic.utils.CommonUtils;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CauseDTO {

	protected String errorMessage;
	protected String stackTrace;

	public CauseDTO(Throwable e) {
		errorMessage = e.getMessage();
		stackTrace = CommonUtils.convertStackTraceToString(e.getStackTrace());
	}
}
