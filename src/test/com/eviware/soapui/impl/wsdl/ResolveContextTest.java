/*
 *  soapUI, copyright (C) 2004-2011 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.impl.wsdl;

import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;

import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.support.Tools;

public class ResolveContextTest
{
	public static junit.framework.Test suite()
	{
		return new JUnit4TestAdapter( ResolveContextTest.class );
	}

	@Test
	public void shouldRelativizePath()
	{
		assertTrue( testFilePath( "test.txt", "c:\\dir\\test.txt", "c:\\dir" ) );
		assertTrue( testFilePath( "dir2\\test.txt", "c:\\dir\\dir2\\test.txt", "c:\\dir" ) );
		assertTrue( testFilePath( "..\\test.txt", "c:\\dir\\dir2\\test.txt", "c:\\dir\\dir2\\dir3" ) );
		assertTrue( testFilePath( "dir\\test.txt", "c:\\dir\\test.txt", "c:\\" ) );
		assertTrue( testFilePath( "..\\test.txt", "c:\\dir\\test.txt", "c:\\dir\\anotherDir" ) );
		assertTrue( testFilePath( "..\\dir2\\test.txt", "c:\\dir\\dir2\\test.txt", "c:\\dir\\anotherDir" ) );

		testUrl( "test.txt", "http://www.test.com/dir/test.txt", "http://www.test.com/dir" );
		testUrl( "dir2/test.txt", "http://www.test.com/dir/dir2/test.txt", "http://www.test.com/dir" );
		testUrl( "../test.txt?test", "http://www.test.com/dir/dir2/test.txt?test", "http://www.test.com/dir/dir2/dir3" );
	}

	private boolean testFilePath( String relativePath, String absolutePath, String rootPath )
	{
		Boolean rValue = relativePath.equals( PathUtils.relativize( absolutePath, rootPath ) );

		if( !rValue )
		{
			return rValue;
		}

		if( !rootPath.endsWith( "\\" ) )
			rootPath += "\\";

		rValue = absolutePath.equals( Tools.joinRelativeUrl( rootPath, relativePath ) );

		return rValue;
	}

	private boolean testUrl( String relativePath, String absolutePath, String rootPath )
	{
		Boolean rValue = relativePath.equals( PathUtils.relativize( absolutePath, rootPath ) );

		if( !rValue )
		{
			return rValue;
		}

		if( !rootPath.endsWith( "/" ) )
			rootPath += "/";

		rValue = absolutePath.equals( Tools.joinRelativeUrl( rootPath, relativePath ) );

		return rValue;
	}
}
