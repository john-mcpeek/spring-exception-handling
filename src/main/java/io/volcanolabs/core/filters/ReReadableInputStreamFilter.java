package io.volcanolabs.core.filters;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * This Filter enables us to re-read the input stream. We need to do this so we can
 * send the user the original request body in the event of an exception being thrown.
 * 
 * @author John McPeek
 *
 */
public class ReReadableInputStreamFilter implements Filter {

	public void init(FilterConfig filterConfig) throws ServletException {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequestWrapper wrappedRequest = new ReReadableHttpServletRequest( (HttpServletRequest) request );
		
		chain.doFilter( wrappedRequest, response );
	}
	
	public void destroy() {
	}
}

class ReReadableHttpServletRequest extends HttpServletRequestWrapper {
	private static final int CHUNK_SIZE = 8192;
	
	private byte[] bodyBytes;
	private ServletInputStream sis;

	public ReReadableHttpServletRequest(HttpServletRequest request) throws IOException {
		super( request );
		
		sis = request.getInputStream();
		ByteArrayOutputStream accumulator = new ByteArrayOutputStream();
		byte[] buffer = new byte[ CHUNK_SIZE ];
		int count = 0;
		while ( ( count = sis.read( buffer ) ) > 0 ) {
			accumulator.write( buffer, 0, count );
		}
		bodyBytes = accumulator.toByteArray();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return new ArrayServletInputStream( bodyBytes, sis );
	}
	
	@Override
	public BufferedReader getReader() throws IOException {
		String bodyStr = new String( bodyBytes );
		
		return new BufferedReader( new CharArrayReader( bodyStr.toCharArray() ) );
	}
}

class ArrayServletInputStream extends ServletInputStream {
	private ByteArrayInputStream bodyBytes;
	private ServletInputStream sis;
	private ReadListener listener;

	public ArrayServletInputStream(byte[] bodyBytesAr, ServletInputStream sis) throws IOException {
		this.bodyBytes = new ByteArrayInputStream( bodyBytesAr );
		this.sis = sis;
	}

	@Override
	public int read() throws IOException {
		int nextByte = bodyBytes.read();
		
		if ( bodyBytes.available() == 0 && listener != null ) {
			listener.onAllDataRead();
		}
		
		return nextByte;
	}

	@Override
	public boolean isFinished() {
		return bodyBytes.available() == 0;
	}

	@Override
	public boolean isReady() {
		return bodyBytes.available() > 0;
	}

	@Override
	public void setReadListener(ReadListener listener) {
		sis.setReadListener( listener );
	}
	
	@Override
	public int available() throws IOException {
		return bodyBytes.available();
	}
	
	@Override
	public void close() throws IOException {
		sis.close();
	}
	
	@Override
	public synchronized void mark(int readlimit) {
		bodyBytes.mark( readlimit );
	}
	
	@Override
	public boolean markSupported() {
		return bodyBytes.markSupported();
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		return bodyBytes.read( b );
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return bodyBytes.read( b, off, len );
	}
	
	@Override
	public int readLine(byte[] b, int off, int len) throws IOException {
        return readLine( b, off, len );
	}
	
	@Override
	public synchronized void reset() throws IOException {
		bodyBytes.reset();
	}
	
	@Override
	public long skip(long n) throws IOException {
		return bodyBytes.skip( n );
	}
}
