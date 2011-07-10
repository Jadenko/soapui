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

package com.eviware.soapui.impl.wsdl.teststeps.assertions.soap;

import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.wsdl.submit.WsdlMessageExchange;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlContext;
import com.eviware.soapui.impl.wsdl.support.wsdl.WsdlValidator;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMockResponseTestStep;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.AbstractTestAssertionFactory;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.RequestAssertion;

/**
 * Asserts that the specified message is a valid SOAP Message
 * 
 * @author ole.matzura
 */

public class SoapRequestAssertion extends WsdlMessageAssertion implements RequestAssertion
{
	public static final String ID = "SOAP Request";
	public static final String LABEL = "SOAP Request";

	public SoapRequestAssertion( TestAssertionConfig assertionConfig, Assertable assertable )
	{
		super( assertionConfig, assertable, false, false, false, false );
	}

	@Override
	protected String internalAssertRequest( MessageExchange messageExchange, SubmitContext context )
			throws AssertionException
	{
		WsdlContext wsdlContext = ( ( WsdlMessageExchange )messageExchange ).getOperation().getInterface()
				.getWsdlContext();
		WsdlValidator validator = new WsdlValidator( wsdlContext );

		try
		{
			AssertionError[] errors = validator.assertRequest( ( WsdlMessageExchange )messageExchange, true );
			if( errors.length > 0 )
				throw new AssertionException( errors );
		}
		catch( AssertionException e )
		{
			throw e;
		}
		catch( Exception e )
		{
			throw new AssertionException( new AssertionError( e.getMessage() ) );
		}

		return "Request Envelope OK";
	}

	public static class Factory extends AbstractTestAssertionFactory
	{
		public Factory()
		{
			super( SoapRequestAssertion.ID, SoapRequestAssertion.LABEL, SoapRequestAssertion.class,
					WsdlMockResponseTestStep.class );
		}

		@Override
		public Class<? extends WsdlMessageAssertion> getAssertionClassType()
		{
			return SoapRequestAssertion.class;
		}
	}

	@Override
	protected String internalAssertResponse( MessageExchange messageExchange, SubmitContext context )
			throws AssertionException
	{
		return null;
	}
}
