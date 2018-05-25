package com.example.chess.dto.output.exceptions;

import com.example.chess.utils.CommonUtils;
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
