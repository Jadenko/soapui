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

package com.eviware.soapui.impl.wsdl.submit.transports.http;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.URI;
import org.apache.log4j.Logger;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.support.AbstractHttpRequestInterface;
import com.eviware.soapui.impl.support.http.HttpRequestInterface;
import com.eviware.soapui.impl.wsdl.AbstractWsdlModelItem;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.impl.wsdl.submit.RequestFilter;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.attachments.MimeMessageResponse;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.methods.ExtendedDeleteMethod;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.methods.ExtendedGetMethod;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.methods.ExtendedHeadMethod;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.methods.ExtendedOptionsMethod;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.methods.ExtendedPostMethod;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.methods.ExtendedPutMethod;
import com.eviware.soapui.impl.wsdl.submit.transports.http.support.methods.ExtendedTraceMethod;
import com.eviware.soapui.impl.wsdl.support.PathUtils;
import com.eviware.soapui.impl.wsdl.support.http.HttpClientSupport;
import com.eviware.soapui.impl.wsdl.support.http.SoapUIHostConfiguration;
import com.eviware.soapui.impl.wsdl.support.wss.WssCrypto;
import com.eviware.soapui.model.iface.Request;
import com.eviware.soapui.model.iface.Response;
import com.eviware.soapui.model.iface.SubmitContext;
import com.eviware.soapui.model.propertyexpansion.PropertyExpander;
import com.eviware.soapui.model.settings.Settings;
import com.eviware.soapui.model.support.ModelSupport;
import com.eviware.soapui.settings.HttpSettings;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.types.StringToStringMap;
import com.eviware.soapui.support.types.StringToStringsMap;

/**
 * HTTP transport that uses HttpClient to send/receive SOAP messages
 * 
 * @author Ole.Matzura
 */

public class HttpClientRequestTransport implements BaseHttpRequestTransport
{
	private List<RequestFilter> filters = new ArrayList<RequestFilter>();
	private final static Logger log = Logger.getLogger( HttpClientRequestTransport.class );

	public HttpClientRequestTransport()
	{
	}

	public void addRequestFilter( RequestFilter filter )
	{
		filters.add( filter );
	}

	public void removeRequestFilter( RequestFilter filter )
	{
		filters.remove( filter );
	}

	public void abortRequest( SubmitContext submitContext )
	{
		HttpMethodBase postMethod = ( HttpMethodBase )submitContext.getProperty( HTTP_METHOD );
		if( postMethod != null )
			postMethod.abort();
	}

	public Response sendRequest( SubmitContext submitContext, Request request ) throws Exception
	{
		AbstractHttpRequestInterface<?> httpRequest = ( AbstractHttpRequestInterface<?> )request;

		HttpClient httpClient = HttpClientSupport.getHttpClient();
		ExtendedHttpMethod httpMethod = createHttpMethod( httpRequest );
		boolean createdState = false;

		HttpState httpState = ( HttpState )submitContext.getProperty( SubmitContext.HTTP_STATE_PROPERTY );
		if( httpState == null )
		{
			httpState = new HttpState();
			submitContext.setProperty( SubmitContext.HTTP_STATE_PROPERTY, httpState );
			createdState = true;
		}

		HostConfiguration hostConfiguration = new HostConfiguration();

		String localAddress = System.getProperty( "soapui.bind.address", httpRequest.getBindAddress() );
		if( localAddress == null || localAddress.trim().length() == 0 )
			localAddress = SoapUI.getSettings().getString( HttpSettings.BIND_ADDRESS, null );

		if( localAddress != null && localAddress.trim().length() > 0 )
		{
			try
			{
				hostConfiguration.setLocalAddress( InetAddress.getByName( localAddress ) );
			}
			catch( Exception e )
			{
				SoapUI.logError( e, "Failed to set localAddress to [" + localAddress + "]" );
			}
		}

		submitContext.removeProperty( RESPONSE );
		submitContext.setProperty( HTTP_METHOD, httpMethod );
		submitContext.setProperty( POST_METHOD, httpMethod );
		submitContext.setProperty( HTTP_CLIENT, httpClient );
		submitContext.setProperty( REQUEST_CONTENT, httpRequest.getRequestContent() );
		submitContext.setProperty( HOST_CONFIGURATION, hostConfiguration );
		submitContext.setProperty( WSDL_REQUEST, httpRequest );
		submitContext.setProperty( RESPONSE_PROPERTIES, new StringToStringMap() );

		for( RequestFilter filter : filters )
		{
			filter.filterRequest( submitContext, httpRequest );
		}

		try
		{
			Settings settings = httpRequest.getSettings();

			// custom http headers last so they can be overridden
			StringToStringsMap headers = httpRequest.getRequestHeaders();

			// first remove so we don't get any unwanted duplicates
			for( String header : headers.keySet() )
			{
				httpMethod.removeRequestHeader( header );
			}

			// now add
			for( String header : headers.keySet() )
			{
				for( String headerValue : headers.get( header ) )
				{
					headerValue = PropertyExpander.expandProperties( submitContext, headerValue );
					httpMethod.addRequestHeader( header, headerValue );
				}
			}

			// do request
			WsdlProject project = ( WsdlProject )ModelSupport.getModelItemProject( httpRequest );
			WssCrypto crypto = null;
			if( project != null && project.getWssContainer() != null )
			{
				crypto = project.getWssContainer().getCryptoByName(
						PropertyExpander.expandProperties( submitContext, httpRequest.getSslKeystore() ) );
			}

			if( crypto != null && WssCrypto.STATUS_OK.equals( crypto.getStatus() ) )
			{
				hostConfiguration.getParams().setParameter( SoapUIHostConfiguration.SOAPUI_SSL_CONFIG,
						crypto.getSource() + " " + crypto.getPassword() );
			}

			// dump file?
			httpMethod.setDumpFile( PathUtils.expandPath( httpRequest.getDumpFile(),
					( AbstractWsdlModelItem<?> )httpRequest, submitContext ) );

			// fix absolute URIs due to peculiarity in httpclient
			URI uri = ( URI )submitContext.getProperty( BaseHttpRequestTransport.REQUEST_URI );
			if( uri != null && uri.isAbsoluteURI() )
			{
				hostConfiguration.setHost( uri.getHost(), uri.getPort() );
				String str = uri.toString();
				int ix = str.indexOf( '/', str.indexOf( "//" ) + 2 );
				if( ix != -1 )
				{
					uri = new URI( str.substring( ix ), true );
					String qs = httpMethod.getQueryString();
					httpMethod.setURI( uri );
					if( StringUtils.hasContent( qs ) )
						httpMethod.setQueryString( qs );

					submitContext.setProperty( BaseHttpRequestTransport.REQUEST_URI, uri );
				}
			}

			// include request time?
			if( settings.getBoolean( HttpSettings.INCLUDE_REQUEST_IN_TIME_TAKEN ) )
				httpMethod.initStartTime();

			// submit!
			httpClient.executeMethod( hostConfiguration, httpMethod, httpState );
			httpMethod.getTimeTaken();

			if( isRedirectResponse( httpMethod ) && httpRequest.isFollowRedirects() )
			{
				ExtendedGetMethod returnMethod = followRedirects( httpClient, 0, httpMethod, httpState );
				httpMethod.releaseConnection();
				httpMethod = returnMethod;
				submitContext.setProperty( HTTP_METHOD, httpMethod );
			}
		}
		catch( Throwable t )
		{
			httpMethod.setFailed( t );

			if( t instanceof Exception )
				throw ( Exception )t;

			SoapUI.logError( t );
			throw new Exception( t );
		}
		finally
		{
			for( int c = filters.size() - 1; c >= 0; c-- )
			{
				RequestFilter filter = filters.get( c );
				filter.afterRequest( submitContext, httpRequest );
			}

			if( !submitContext.hasProperty( RESPONSE ) )
			{
				createDefaultResponse( submitContext, httpRequest, httpMethod );
			}

			Response response = ( Response )submitContext.getProperty( BaseHttpRequestTransport.RESPONSE );
			StringToStringMap responseProperties = ( StringToStringMap )submitContext
					.getProperty( BaseHttpRequestTransport.RESPONSE_PROPERTIES );

			for( String key : responseProperties.keySet() )
			{
				response.setProperty( key, responseProperties.get( key ) );
			}

			if( httpMethod != null )
			{
				httpMethod.releaseConnection();
			}
			else
				log.error( "PostMethod is null" );

			if( createdState )
			{
				submitContext.setProperty( SubmitContext.HTTP_STATE_PROPERTY, null );
			}
		}

		return ( Response )submitContext.getProperty( BaseHttpRequestTransport.RESPONSE );
	}

	private boolean isRedirectResponse( ExtendedHttpMethod httpMethod )
	{
		switch( httpMethod.getStatusCode() )
		{
		case 301 :
		case 302 :
		case 303 :
		case 307 :
			return true;
		}

		return false;
	}

	private ExtendedGetMethod followRedirects( HttpClient httpClient, int redirectCount, ExtendedHttpMethod httpMethod,
			HttpState httpState ) throws Exception
	{
		ExtendedGetMethod getMethod = new ExtendedGetMethod();
		for( Header header : httpMethod.getRequestHeaders() )
			getMethod.addRequestHeader( header );

		URI uri = new URI( httpMethod.getResponseHeader( "Location" ).getValue(), true );
		getMethod.setURI( uri );
		HostConfiguration host = new HostConfiguration();
		host.setHost( uri );
		httpClient.executeMethod( host, getMethod, httpState );
		if( isRedirectResponse( getMethod ) )
		{
			if( redirectCount == 10 )
				throw new Exception( "Maximum number of Redirects reached [10]" );

			try
			{
				return followRedirects( httpClient, redirectCount + 1, getMethod, httpState );
			}
			finally
			{
				getMethod.releaseConnection();
			}
		}
		else
			return getMethod;

	}

	private void createDefaultResponse( SubmitContext submitContext, AbstractHttpRequestInterface<?> httpRequest,
			ExtendedHttpMethod httpMethod )
	{
		String requestContent = ( String )submitContext.getProperty( BaseHttpRequestTransport.REQUEST_CONTENT );

		// check content-type for multiplart
		Header responseContentTypeHeader = httpMethod.getResponseHeader( "Content-Type" );
		Response response = null;

		if( responseContentTypeHeader != null
				&& responseContentTypeHeader.getValue().toUpperCase().startsWith( "MULTIPART" ) )
		{
			response = new MimeMessageResponse( httpRequest, httpMethod, requestContent, submitContext );
		}
		else
		{
			response = new SinglePartHttpResponse( httpRequest, httpMethod, requestContent, submitContext );
		}

		submitContext.setProperty( BaseHttpRequestTransport.RESPONSE, response );
	}

	private ExtendedHttpMethod createHttpMethod( AbstractHttpRequestInterface<?> httpRequest )
	{
		if( httpRequest instanceof HttpRequestInterface<?> )
		{
			HttpRequestInterface<?> restRequest = ( HttpRequestInterface<?> )httpRequest;
			switch( restRequest.getMethod() )
			{
			case GET :
				return new ExtendedGetMethod();
			case HEAD :
				return new ExtendedHeadMethod();
			case DELETE :
				return new ExtendedDeleteMethod();
			case PUT :
				return new ExtendedPutMethod();
			case OPTIONS :
				return new ExtendedOptionsMethod();
			case TRACE :
				return new ExtendedTraceMethod();
			}
		}

		ExtendedPostMethod extendedPostMethod = new ExtendedPostMethod();

		extendedPostMethod.setAfterRequestInjection( httpRequest.getAfterRequestInjection() );
		return extendedPostMethod;
	}

}
