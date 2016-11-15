package io.volcanolabs.core.exceptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author John McPeek
 *
 */
@ControllerAdvice
public class BaseGlobalExceptionHandler {
	Logger log = LoggerFactory.getLogger( BaseGlobalExceptionHandler.class );

	private static final int CHUNK_SIZE = 8192;

	@ExceptionHandler( value = Exception.class )
	public ResponseEntity<String> defaultErrorHandler(HttpServletRequest request, Exception e) throws Exception {
		// If the exception is annotated with @ResponseStatus re-throw it and let the framework
		// handle it. AnnotationUtils is a Spring Framework utility class.
		if ( AnnotationUtils.findAnnotation( e.getClass(), ResponseStatus.class ) != null ) {
			throw e;
		}

		ResponseEntity<String> result;
		if ( e instanceof SimpleStatusResponseException ) {
			result = generateExceptionResponse( request, ( (SimpleStatusResponseException) e ).getStatusCode(), e );
		} else {
			result = generateExceptionResponse( request, HttpStatus.INTERNAL_SERVER_ERROR, e );
		}

		return result;
	}

	protected ResponseEntity<String> generateExceptionResponse(HttpServletRequest request, HttpStatus statusCode, Exception e) {
		String originalUrl = getOriginalUrl( request );
		String headers = getHeaders( request );
		String body = getBodyOfRequest( request );

		String result = "{\n\t\"statusCode\": \"" + statusCode + "\",\n\t\"message\": \"" + e.getMessage() + "\",\n\t\"url\": \"" + originalUrl + "\",\n\t\"headers\": [\"" + headers + "\"],\n\t\"body\": \"" + body + "\"";
		if ( e != null ) {
			String stackTrace = ExceptionUtils.getStackTrace( e );
			stackTrace = StringEscapeUtils.ESCAPE_JSON.translate( stackTrace );
			result += ",\n\t\"exception\": \"\n" + stackTrace;
		}
		result += "\n}";
		
		log.error( result );

		return new ResponseEntity<String>( result, statusCode );
	}
	
	protected String getOriginalUrl(HttpServletRequest request) {
		String query = request.getQueryString();
		String originalUrl = request.getRequestURI() + ( query == null ? "" : "?" + query );
		
		return originalUrl;
	}

	protected String getBodyOfRequest(HttpServletRequest request) {
		ByteArrayOutputStream accumulator = new ByteArrayOutputStream();

		try {
			InputStream sis = request.getInputStream();
			byte[] buffer = new byte[ CHUNK_SIZE ];
			int count = 0;
			while ( ( count = sis.read( buffer ) ) > 0 ) {
				accumulator.write( buffer, 0, count );
			}
		} catch (IOException e) {
		}

		String body = accumulator.toString();

		return body;
	}

	protected String getHeaders(HttpServletRequest request) {
		Enumeration<?> i = request.getHeaderNames();

		StringBuilder headers = new StringBuilder();
		while ( i.hasMoreElements() ) {
			String header = (String) i.nextElement();
			
			if ( header.equalsIgnoreCase( "authorization" ) ) {
				continue;
			}
			
			String headerValues = "";
			Enumeration<?> enumeration = request.getHeaders( header );
			while ( enumeration.hasMoreElements() ) {
				headerValues += (String) enumeration.nextElement() + ";";
			}
			
			headers.append( header ).append( ": " ).append( headerValues ).append( ", " );
		}

		String headersStr = headers.toString();
		if ( headersStr.length() > 0 ) {
			headersStr = headersStr.substring( 0, headersStr.length() - 2 );
		}

		return headersStr;
	}
}
