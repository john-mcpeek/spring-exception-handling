package io.volcanolabs.core.filters;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

public class FilterTests {
	private final byte[] inputBytes = "123\n456".getBytes();

	@Test
	public void test() throws IOException {		
		@SuppressWarnings( "resource" )
		ArrayServletInputStream arInputStream = new ArrayServletInputStream( inputBytes, null );
		
		byte[] readBytes = new byte[ inputBytes.length ];
		arInputStream.readLine( readBytes, 0, inputBytes.length );
		
		byte[] expectedBytes = "123\n###".getBytes();
		Arrays.fill( expectedBytes, 4, 7, (byte) 0 );
		assertArrayEquals( expectedBytes, readBytes );
	}

}