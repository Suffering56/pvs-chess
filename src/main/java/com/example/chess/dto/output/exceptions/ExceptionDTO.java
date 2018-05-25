package com.example.chess.dto.output.exceptions;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExceptionDTO extends CauseDTO {

	protected CauseDTO cause;

	public ExceptionDTO(Throwable e) {
		super(e);
		if (e.getCause() != null) {
			cause = new CauseDTO(e.getCause());
		}
	}
}

