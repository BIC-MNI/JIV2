
// $Id: Util.java,v 1.4 2001-09-26 03:07:28 cc Exp $
/* 
  This file is part of JIV.  
  Copyright (C) 2000, 2001 Chris A. Cocosco (crisco@bic.mni.mcgill.ca)

  JIV is free software; you can redistribute it and/or modify it under
  the terms of the GNU General Public License as published by the Free
  Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  JIV is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
  License for more details.

  You should have received a copy of the GNU General Public License
  along with JIV; if not, write to the Free Software Foundation, Inc.,
  59 Temple Place, Suite 330, Boston, MA 02111-1307 USA, 
  or see http://www.gnu.org/copyleft/gpl.html
*/


package jiv;

import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.zip.*;

/**
 * A collection of various (<code>static</code>) utility functions.
 *
 * @author Chris Cocosco (crisco@bic.mni.mcgill.ca)
 * @version $Id: Util.java,v 1.4 2001-09-26 03:07:28 cc Exp $
 */
public final class Util {

    /** used by reduceSignificantDigits() */
    /*private*/ static final int[] __chopFractionalPart_lookup= {
	1, 10, 100, 1000, 10000, 100000
    };

    /** Limitation: (original * 10^no_of_digits) has to fit in a
	signed integer (java integers are 32bit)
    */
    public static final float chopFractionalPart( float original,
						  int no_of_digits /** 0...5 */
						  ) {
	// use a lookup table for speed
	int mult= __chopFractionalPart_lookup[ no_of_digits];
	return Math.round( original * mult) / (float)mult;
    }

    /**
     * @return first enclosing <code>Frame</code> encountered up the
     * component hierarchy
     */
    public static final Frame getParentFrame( Component component) {
	
	Component f= component;
	while( f != null && !( f instanceof Frame ) ) 
	    f= f.getParent();
	return (Frame)f;
    }


    public static final String[] compress_extension= { ".gz", ".bz2" };

    /**
     * @return "extension" of a filename (or url), ignoring '.gz' and '.bz2'
     */
    public static final String getExtension( String s) {
	
	String ext= "";
	for( int i= 0; i < compress_extension.length; ++i) 
	    if( s.endsWith( compress_extension[ i]) ) {
		ext= compress_extension[ i];
		int chop= s.lastIndexOf( compress_extension[ i]);
		s= s.substring( 0, chop);
		break;
	    }
	int ei= s.lastIndexOf( '.');
	if( ei >= 0 ) 
	    return s.substring( ei) + ext;
	else
	    // hmm, no extension really...
	    return ext;
    }

    /** sleeps the current thread, and (unlike Thread.sleep) doesn't throw
	an exception if interrupted. 
    */
    public static final void sleep( long millis ) {

	try {
	    Thread.sleep( millis);
	}
	catch( InterruptedException e) {}
    }


    /** 
	Note1: it is the caller's responsibility to close the returned
	InputStream.

	Note2: it's not _required_ to declare SecurityException (since
	it's a subclass of RuntimeException), but we do it for clarity
	-- this error is likely to happen when working with url-s, so
	it should be treated as a "checked exception" ...  
    */
    public static final InputStream openURL( URL source_url) 
	throws IOException, SecurityException  {

	InputStream input_stream= null;

	URLConnection url_connection= source_url.openConnection();
	// NB: the connection is not yet opened at this point; it'll be 
	// actually done when calling getInputStream() below.
	url_connection.setUseCaches( true);

	if( url_connection instanceof HttpURLConnection ) {
	    HttpURLConnection http_conn= (HttpURLConnection)url_connection;
	    if( http_conn.getResponseCode() != HttpURLConnection.HTTP_OK)
		throw new IOException( source_url.toString() + " : "
				       + http_conn.getResponseCode() + " " 
				       + http_conn.getResponseMessage() );
	}	
	input_stream= url_connection.getInputStream();
	if( source_url.toString().endsWith( ".gz")) 
	    // use a larger decompression buffer than the 512 default
	    input_stream= new GZIPInputStream( input_stream, 4096);

	// TODO: I once got a "IOException: server status 206" when loading
	// the applet via http ... but couldn't reproduce it ever since.
	// what went wrong?
	// (btw, 206 == HTTP_PARTIAL : 
	// "HTTP response code that means the partial request has been fulfilled") 

	return input_stream;
    }


    public static final String arrayToString( byte[] arg) {

	StringBuffer s= new StringBuffer( arg.toString() );
	s.append( " : ");
	for( int i= 0; i < arg.length; ++i) {
	    s.append( arg[ i]);
	    s.append( " ");
	}
	return s.toString();
    }

    public static final String arrayToString( int[] arg) {

	StringBuffer s= new StringBuffer( arg.toString() );
	s.append( " : ");
	for( int i= 0; i < arg.length; ++i) {
	    s.append( arg[ i]);
	    s.append( " ");
	}
	return s.toString();
    }

} // end of class Util
