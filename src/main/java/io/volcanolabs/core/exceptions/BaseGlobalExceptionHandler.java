package io.volcanolabs.core.exceptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Extend this class and add a @ControllerAdvice annotation to get the global exception handling.
 * @author John McPeek
 *
 */
public abstract class BaseGlobalExceptionHandler {

	private static final int CHUNK_SIZE = 8192;

	@ExceptionHandler( value = Exception.class )
	public ResponseEntity<String> defaultErrorHandler(HttpServletRequest request, Exception e) throws Exception {
		// If the exception is annotated with @ResponseStatus re-throw it and let the framework
		// handle it. AnnotationUtils is a Spring Framework utility class.
		if ( AnnotationUtils.findAnnotation( e.getClass(), ResponseStatus.class ) != null ) {
			throw e;
		}

		ResponseEntity<String> result;
		if ( e instanceof GlobalException ) {
			result = restExceptionHandler( request, (GlobalException) e );
		} else {
			result = generateResponse( request, e );
		}

		return result;
	}

	/**
	 * Override this method to add your own global exception handling behavior.
	 * 
	 * @param request
	 * @param e
	 * @return
	 */
	protected ResponseEntity<String> generateResponse(HttpServletRequest request, Exception e) {
		return defaultResponseGenerator( request, e );
	}

	protected ResponseEntity<String> defaultResponseGenerator(HttpServletRequest request, Exception e) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter( out );		
		e.printStackTrace( pw );
		
		String originalUrl = getOriginalUrl( request );
		String body = getBodyOfRequest( request );
		
		String result = "{\n"
				+ "	\"url\": \"" + originalUrl + "\"\n"
				+ "	\"body\": \"" + body + "\"\n"
				+ "	\"exception\": \"" + e.getClass().getName() + "\"\n"
				+ "	\"message\": \"" + e.getMessage() + "\"\n"
				+ "	\"stacktrace\": \"" + out.toString() + "\"\n"
				+ "}";
		
		return new ResponseEntity<>( result, HttpStatus.INTERNAL_SERVER_ERROR );
	}
	
	protected ResponseEntity<String> restExceptionHandler(HttpServletRequest request, GlobalException e) {
		String originalUrl = getOriginalUrl( request );
		String body = getBodyOfRequest( request );
		
		String result = "{"
				+ "	\"message\": \"" + e.getMessage() + "\"\n"  
				+ "	\"url\": \"" + originalUrl + "\"\n"
				+ "	\"body\": \"" + body + "\""
				+ "}";
				
		return new ResponseEntity<>( result, e.getStatusCode() );
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
}
