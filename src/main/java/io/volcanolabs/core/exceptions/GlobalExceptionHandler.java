package io.volcanolabs.core.exceptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author John McPeek
 *
 */
@ControllerAdvice
public class GlobalExceptionHandler {
	protected static final Logger log = LoggerFactory.getLogger( GlobalExceptionHandler.class );
	protected static final Set<String> headersToHide = new HashSet<>();
	protected static final int CHUNK_SIZE = 8192;
	
	@Value( "${http.headers.to_hide}" )
	private String[] headersToHideConfig;
	
	public GlobalExceptionHandler() {
		log.info( "GlobalExceptionHandler created." );
	}
	
	@PostConstruct
	public void init() {
		log.info( "Loading headersToHide: {}", Arrays.toString( headersToHideConfig ) );
		headersToHide.addAll( Arrays.asList( headersToHideConfig ) );
	}

	@ExceptionHandler( value = Exception.class )
	public ResponseEntity<String> defaultErrorHandler(HttpServletRequest request, Exception e) throws Exception {
		// If the exception is annotated with @ResponseStatus re-throw it and let the framework
		// handle it. AnnotationUtils is a Spring Framework utility class.
		if ( AnnotationUtils.findAnnotation( e.getClass(), ResponseStatus.class ) != null ) {
			throw e;
		}

		if ( e instanceof NoHandlerFoundException ) {
			e = new SimpleStatusResponseException( e.getMessage(), HttpStatus.BAD_REQUEST );
		}
		else if ( e.getCause() instanceof JsonMappingException) {
			e = new SimpleStatusResponseException( e.getCause().getMessage(), HttpStatus.BAD_REQUEST );
		}
		
		HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
		if ( e instanceof SimpleStatusResponseException ) {
			statusCode = ( (SimpleStatusResponseException) e ).getStatusCode();
		}

		ResponseEntity<String> result = generateExceptionResponse( request, statusCode, e );
		
		return result;
	}

	protected ResponseEntity<String> generateExceptionResponse(HttpServletRequest request, HttpStatus statusCode, Exception e) {
		String originalUrl = getOriginalUrl( request );
		String headers = getHeaders( request );
		String body = getBodyOfRequest( request );

		String result = "{\n\t\""
				+ "statusCode\": \"" + statusCode + "\",\n\t\""
				+ "message\": \"" + e.getMessage() + "\",\n\t\""
				+ "method\": \"" + request.getMethod() + "\",\n\t\""
				+ "url\": \"" + originalUrl + "\",\n\t\""
				+ "headers\": [\"" + headers + "\"],\n\t\""
				+ "body\": \"" + body + "\"";
		if ( e instanceof SimpleStatusResponseException == false ) {
			String stackTrace = ExceptionUtils.getStackTrace( e );
			stackTrace = StringEscapeUtils.ESCAPE_JSON.translate( stackTrace );
			result += ",\n\t\"exception\": \"\n" + stackTrace + "\"";
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
			
			String headerValues = "";
			Enumeration<?> enumeration = request.getHeaders( header );
			while ( enumeration.hasMoreElements() ) {
				String headerValue = (String) enumeration.nextElement();
				if ( headersToHide.contains( header.toLowerCase() ) ) {
					headerValue = headerValue.replaceAll( ".", "X" );
				}
				headerValues += headerValue + ";";
			}
			
			headers.append( header ).append( ": " ).append( headerValues ).append( " " );
		}

		String headersStr = headers.toString();
		if ( headersStr.length() > 0 ) {
			headersStr = headersStr.substring( 0, headersStr.length() - 1 );
		}

		return headersStr;
	}
}
