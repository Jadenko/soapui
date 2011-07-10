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

package com.eviware.soapui.security.assertion;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlObject;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.config.TestAssertionConfig;
import com.eviware.soapui.impl.support.AbstractHttpRequest;
import com.eviware.soapui.impl.wsdl.support.HelpUrls;
import com.eviware.soapui.impl.wsdl.teststeps.WsdlMessageAssertion;
import com.eviware.soapui.impl.wsdl.teststeps.assertions.AbstractTestAssertionFactory;
import com.eviware.soapui.model.iface.MessageExchange;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.security.SecurityScan;
import com.eviware.soapui.model.testsuite.Assertable;
import com.eviware.soapui.model.testsuite.AssertionError;
import com.eviware.soapui.model.testsuite.AssertionException;
import com.eviware.soapui.model.testsuite.ResponseAssertion;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.xml.XmlObjectConfigurationBuilder;
import com.eviware.soapui.support.xml.XmlObjectConfigurationReader;
import com.eviware.x.form.XForm;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormDialogBuilder;
import com.eviware.x.form.XFormFactory;

/**
 * Asserts Http status code in response
 * 
 * @author nebojsa.tasic
 */

public class InvalidHttpStatusCodesAssertion extends WsdlMessageAssertion implements ResponseAssertion
{
	public static final String ID = "Invalid HTTP Status Codes";
	public static final String LABEL = "Invalid HTTP Status Codes";

	private String codes;
	private XFormDialog dialog;
	private static final String CODES = "codes";

	public InvalidHttpStatusCodesAssertion( TestAssertionConfig assertionConfig, Assertable assertable )
	{
		super( assertionConfig, assertable, false, true, false, true );

		init();
	}

	private void init()
	{
		XmlObjectConfigurationReader reader = new XmlObjectConfigurationReader( getConfiguration() );
		codes = reader.readString( CODES, "" );
	}

	@Override
	protected String internalAssertResponse( MessageExchange messageExchange, SubmitContext context )
			throws AssertionException
	{

		List<String> codeList = extractCodes( context );
		String[] statusElements = null;
		try
		{
			statusElements = messageExchange.getResponseHeaders().get( "#status#", "-1" ).split( " " );

		}
		catch( NullPointerException npe )
		{
			SoapUI.logError( npe, "Header #status# is missing!" );
		}

		if( statusElements.length >= 2 )
		{
			String statusCode = statusElements[1].trim();
			if( codeList.contains( statusCode ) )
			{
				String message = "Response status code: " + statusCode + " is in invalid list of status codes";
				throw new AssertionException( new AssertionError( message ) );
			}
		}
		else
		{
			throw new AssertionException( new AssertionError( "Status code extraction error! " ) );
		}

		return "OK";
	}

	private List<String> extractCodes( SubmitContext context )
	{
		String expandedCodes = context.expand( codes );
		List<String> codeList = new ArrayList<String>();
		for( String str : expandedCodes.split( "," ) )
		{
			codeList.add( str.trim() );
		}
		return codeList;
	}

	public static class Factory extends AbstractTestAssertionFactory
	{
		@SuppressWarnings( "unchecked" )
		public Factory()
		{
			super( InvalidHttpStatusCodesAssertion.ID, InvalidHttpStatusCodesAssertion.LABEL,
					InvalidHttpStatusCodesAssertion.class, new Class[] { SecurityScan.class, AbstractHttpRequest.class } );
		}

		@Override
		public Class<? extends WsdlMessageAssertion> getAssertionClassType()
		{
			return InvalidHttpStatusCodesAssertion.class;
		}
	}

	@Override
	protected String internalAssertRequest( MessageExchange messageExchange, SubmitContext context )
			throws AssertionException
	{
		return null;
	}

	protected XmlObject createConfiguration()
	{
		XmlObjectConfigurationBuilder builder = new XmlObjectConfigurationBuilder();
		builder.add( CODES, codes );
		return builder.finish();
	}

	@Override
	public boolean configure()
	{
		if( dialog == null )
			buildDialog();

		StringToStringMap values = new StringToStringMap();
		values.put( CODES, codes );

		values = dialog.show( values );
		if( dialog.getReturnValue() == XFormDialog.OK_OPTION )
		{
			codes = values.get( CODES );
		}

		setConfiguration( createConfiguration() );
		return true;
	}

	private void buildDialog()
	{
		XFormDialogBuilder builder = XFormFactory.createDialogBuilder( "Invalid HTTP status codes Assertion" );
		XForm mainForm = builder.createForm( "Basic" );

		mainForm.addTextField( CODES, "Comma-separated not acceptable status codes", XForm.FieldType.TEXTAREA ).setWidth(
				40 );

		// TODO : update help URL
		dialog = builder.buildDialog(
				builder.buildOkCancelHelpActions( HelpUrls.SECURITY_INVALID_HTTP_CODES_ASSERTION_HELP ), "Specify codes",
				UISupport.OPTIONS_ICON );
	}

}
