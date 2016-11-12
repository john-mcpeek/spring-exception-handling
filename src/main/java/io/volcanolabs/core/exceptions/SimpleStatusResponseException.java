package io.volcanolabs.core.exceptions;

import org.springframework.http.HttpStatus;

/**
 * @author John McPeek
 *
 */
public class SimpleStatusResponseException extends RuntimeException {
	private HttpStatus statusCode;

	public SimpleStatusResponseException(String message, HttpStatus statusCode) {
		super( message );

		this.statusCode = statusCode;
	}

	public SimpleStatusResponseException(String message, Throwable cause, HttpStatus statusCode) {
		super( message, cause );

		this.statusCode = statusCode;
	}

	public SimpleStatusResponseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, HttpStatus statusCode) {
		super( message, cause, enableSuppression, writableStackTrace );

		this.statusCode = statusCode;
	}

	public HttpStatus getStatusCode() {
		return statusCode;
	}
}
