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

package com.eviware.soapui.impl.wsdl.support.http;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;

public class ProxyUtilsTestCase
{
	public static junit.framework.Test suite()
	{
		return new JUnit4TestAdapter( ProxyUtilsTestCase.class );
	}

	@Test
	public void testExcludes()
	{
		assertFalse( ProxyUtils.excludes( new String[] { "" }, "www.test.com", 8080 ) );
		assertTrue( ProxyUtils.excludes( new String[] { "test.com" }, "www.test.com", 8080 ) );
		assertFalse( ProxyUtils.excludes( new String[] { "test2.com" }, "www.test.com", 8080 ) );
		assertTrue( ProxyUtils.excludes( new String[] { "test.com:8080" }, "www.test.com", 8080 ) );
		assertFalse( ProxyUtils.excludes( new String[] { "test2.com:8080" }, "www.test.com", 8080 ) );
		assertFalse( ProxyUtils.excludes( new String[] { "test.com:8081" }, "www.test.com", 8080 ) );
		assertTrue( ProxyUtils.excludes( new String[] { "test.com:8080", "test.com:8081" }, "www.test.com", 8080 ) );
		assertTrue( ProxyUtils.excludes( new String[] { "test.com:8080", "test.com" }, "www.test.com", 8080 ) );
	}
}
