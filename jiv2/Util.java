/* 
  This file is part of JIV2.  
  Copyright (C) 2000, 2001 Chris A. Cocosco (crisco@bic.mni.mcgill.ca),
  2010 Lara Bailey (bailey@bic.mni.mcgill.ca).

  JIV2 is free software; you can redistribute it and/or modify it under
  the terms of the GNU General Public License as published by the Free
  Software Foundation; either version 2 of the License, or (at your
  option) any later version.

  JIV2 is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
  License for more details.

  You should have received a copy of the GNU General Public License
  along with JIV2; if not, write to the Free Software Foundation, Inc.,
  59 Temple Place, Suite 330, Boston, MA 02111-1307 USA, 
  or see http://www.gnu.org/copyleft/gpl.html
*/


package jiv2;

import java.awt.*;
import java.net.*;
import java.io.*;
import java.util.zip.*;
import java.util.*;

/**
 * A collection of various (<code>static</code>) utility functions.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: Util.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class Util {

    /** shows which files are loaded */
    /*private*/ static final boolean    VERBOSE= true;

    /*private*/ static final boolean    DEBUG= false;
    /*private*/ static final boolean    DEBUG_INVERSE= false;

    /** Precision value used for comparing floats */
    /*private*/ static final float    EPSILON= 0.00000001f;

    /*private*/ static final int    MATRIX_SIZE_1D= 12;
    /*private*/ static final int    MATRIX_SIZE_2D_1= 3;
    /*private*/ static final int    MATRIX_SIZE_2D_2= 4;

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


    /*private*/ static final float _log_e_10= (float)Math.log( 10.0);

    public static final float chopToNSignificantDigits( final float original,
							int no_of_sig_digits 
							) {
	if( original == 0f)
	    return 0f;

	/* a plain float->int conversion (which is a 'trunc' in java)
           instead of the 'floor' won't be correct if the argument is
           negative (eg original = 1.23e-7) */
	final int exp= 
	    (int)Math.floor( (float)Math.log( Math.abs( original)) 
			     / _log_e_10
			     - no_of_sig_digits + 1f 
			     );

	/* it's important to use double here! else, '1e{exp}' may be
           <Float.MIN_VALUE for a very small 'original' */
	final double mult= Math.pow( 10.0, exp);

	return (float)( Math.round( original / mult) * mult );
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

	if( DEBUG ) { System.out.println( "Util::openUrl( " + source_url +" )"); }

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


    /** @return the Properties object described by the file at
	source_url; trailing whitespace is trimmed off the property
	values (the stock Java Properties.load() doesn't do it!)  
    */
    public static final Properties readProperties( URL source_url, 
						   Properties defaults) 
	throws IOException, SecurityException  {

	if( source_url == null) 
	    return null;

	InputStream input_stream= null;
	try {
	    input_stream= Util.openURL( source_url);
	    return _readAndTrimProperties( input_stream, defaults);
	}
	finally {
	    if( input_stream != null) {
		// TODO: what if we try to close() a stream that wasn't 
		// successfully opened? is this a problem?
		input_stream.close();
	    }
	}
    }

    /** @return the Properties object described inline in the source
	String (with ';' instead of newlines); trailing whitespace is
	trimmed off the property values (the stock Java
	Properties.load() doesn't do it!)  
    */
    public static final Properties readProperties( final String src, 
						   Properties defaults) 
	throws IOException {

	PipedOutputStream os= new PipedOutputStream();
	PipedInputStream is= new PipedInputStream( os);
	final Writer pw= new BufferedWriter( new OutputStreamWriter( os));

	// cannot have a pair of Piped...Stream-s in the same thread
	// (deadlock danger)
	Thread t= new Thread( new Runnable() {
		public void run() { 
		    try {
			String lineSeparator = System.getProperty("line.separator");
			// replace ';' with newline
			StringTokenizer lines= new StringTokenizer( src, ";", false);
			while( lines.hasMoreTokens() ) {
			    pw.write( lines.nextToken() + lineSeparator);
			    pw.flush();
			}
			pw.close();
		    }
		    catch( Exception x) {
			System.err.println( "Exception (" + x + ") when writing to pipe in readProperties( " + src + " )");
		    }
		}
	    });

	try {
	    t.start();
	    Properties ret= _readAndTrimProperties( is, defaults);
	    return ret;
	}
	finally {
	    is.close(); os.close(); 
	}
    }
    
    /** @return trims trailing whitespace off the values (the stock Java
	Properties.load() doesn't do it!)  
    */
    /*private*/ static final Properties _readAndTrimProperties( InputStream src,
								Properties defaults) 
	throws IOException {

	Properties raw= new Properties( defaults);
	raw.load( src);
	Enumeration prop_names;
	Properties ret= new Properties();
	for( prop_names= raw.propertyNames(); prop_names.hasMoreElements(); ) {
	    String key= (String)prop_names.nextElement();
	    ret.put( key, raw.getProperty( key).trim());
	}
	return ret;
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

    public static final String arrayToString( float[] arg) {

	StringBuffer s= new StringBuffer( arg.toString() );
	s.append( " : ");
	for( int i= 0; i < arg.length; ++i) {
	    s.append( arg[ i]);
	    s.append( " ");
	}
	return s.toString();
    }

    public static final String arrayToString( float[][] arg) {

	StringBuffer s= new StringBuffer( arg.toString() );
	s.append( " : ");
	for( int i= 0; i < arg.length; ++i) {
	    for( int j= 0; j < arg[i].length; ++j) {
		s.append( arg[i][j]);
		s.append( " ");
	    }
	}
	return s.toString();
    }

    static final float[] readMatrix( URL source_url, String xfmFileName) 
		throws IOException
    {

	URL transformPathURL;
	float [] txfm_matrix= new float[MATRIX_SIZE_1D];

	InputStream input_stream= null;
//	try {
		// "/path_to_data/monkey_001_nat2mni.xfm" OR
		// "/path_to_data/monkey_001_nat2mni_inv.xfm" OR ...
		transformPathURL= new URL( source_url, xfmFileName);

		input_stream= Util.openURL( transformPathURL);
		BufferedReader br = new BufferedReader(new InputStreamReader(input_stream));
		String strLine = null;
		//Skip text beginning of .xfm
		while ((strLine = br.readLine()) != null){
			if (strLine.equals("Linear_Transform ="))
				break;
		}

		//Read matrix in
		int i= 0;
		while ((strLine = br.readLine()) != null) {
			StringTokenizer name_tokens= new StringTokenizer( strLine, " ", false);
			while( name_tokens.hasMoreTokens()) {
				String str_num= name_tokens.nextToken();
				if (i==11) {
					str_num= str_num.substring(0,str_num.length()-1);
				}
				txfm_matrix[i] = Float.valueOf(str_num);
				if (DEBUG) System.out.println("["+i+"] = "+txfm_matrix[i]);
				i++;
				if (i > 12) {
					System.out.println("Error!! Unexpected additional text in xfm file: "+xfmFileName);
					return new float[]{0f};
				}
			}// end while hasMoreTokens
	
		}// end while readLine != null
		if (i < 12) {
			System.out.println("Error!! Unexpected ending in xfm file: "+xfmFileName);
			return new float[]{0f};
		}

		//Close reader
		br.close();
		input_stream.close();
//	}// end try
//	catch(FileNotFoundException e) {
//		System.out.println( e);
//		return null;
//	}
//	catch(IOException e) {
//		progressMessage("Warning! Problem reading the .xfm file: "+xfmFileName+"\n\t" + e);
//		return null;
//	}
	if (VERBOSE) System.out.println( transformPathURL + " loading done!");
	return txfm_matrix;
    }// end readMatrix



    public static final float[] addTxfmMatrixCosines(float [] trans, float [] pre_cosines, float[] post_cosines) {
	float [] tmp_matrix= new float [MATRIX_SIZE_1D];
	float [] new_matrix= new float [MATRIX_SIZE_1D];
	if (DEBUG) {
		System.out.println("addTxfmMatrixCosines:");
		System.out.println(Util.arrayToString( trans));
		System.out.println(Util.arrayToString( pre_cosines));
	}

	//First add the pre-multiply cosines (native or atlas) -> multiply on right:
	tmp_matrix[0]= trans[0]*pre_cosines[0] + trans[1]*pre_cosines[3] + trans[2]*pre_cosines[6];
	tmp_matrix[1]= trans[0]*pre_cosines[1] + trans[1]*pre_cosines[4] + trans[2]*pre_cosines[7];
	tmp_matrix[2]= trans[0]*pre_cosines[2] + trans[1]*pre_cosines[5] + trans[2]*pre_cosines[8];
	tmp_matrix[3]= trans[3];
	tmp_matrix[4]= trans[4]*pre_cosines[0] + trans[5]*pre_cosines[3] + trans[6]*pre_cosines[6];
	tmp_matrix[5]= trans[4]*pre_cosines[1] + trans[5]*pre_cosines[4] + trans[6]*pre_cosines[7];
	tmp_matrix[6]= trans[4]*pre_cosines[2] + trans[5]*pre_cosines[5] + trans[6]*pre_cosines[8];
	tmp_matrix[7]= trans[7];
	tmp_matrix[8]= trans[8]*pre_cosines[0] + trans[9]*pre_cosines[3] + trans[10]*pre_cosines[6];
	tmp_matrix[9]= trans[8]*pre_cosines[1] + trans[9]*pre_cosines[4] + trans[10]*pre_cosines[7];
	tmp_matrix[10]= trans[8]*pre_cosines[2] + trans[9]*pre_cosines[5] + trans[10]*pre_cosines[8];
	tmp_matrix[11]= trans[11];

	//Then add the post-multiply cosines (mni/template) -> multiply on left:
	new_matrix[0]= post_cosines[0]*tmp_matrix[0] + post_cosines[1]*tmp_matrix[4] + post_cosines[2]*tmp_matrix[8];
	new_matrix[1]= post_cosines[0]*tmp_matrix[1] + post_cosines[1]*tmp_matrix[5] + post_cosines[2]*tmp_matrix[9];
	new_matrix[2]= post_cosines[0]*tmp_matrix[2] + post_cosines[1]*tmp_matrix[6] + post_cosines[2]*tmp_matrix[10];
	new_matrix[3]= post_cosines[0]*tmp_matrix[3] + post_cosines[1]*tmp_matrix[7] + post_cosines[2]*tmp_matrix[11];
	new_matrix[4]= post_cosines[3]*tmp_matrix[0] + post_cosines[4]*tmp_matrix[4] + post_cosines[5]*tmp_matrix[8];
	new_matrix[5]= post_cosines[3]*tmp_matrix[1] + post_cosines[4]*tmp_matrix[5] + post_cosines[5]*tmp_matrix[9];
	new_matrix[6]= post_cosines[3]*tmp_matrix[2] + post_cosines[4]*tmp_matrix[6] + post_cosines[5]*tmp_matrix[10];
	new_matrix[7]= post_cosines[3]*tmp_matrix[3] + post_cosines[4]*tmp_matrix[7] + post_cosines[5]*tmp_matrix[11];
	new_matrix[8]= post_cosines[6]*tmp_matrix[0] + post_cosines[7]*tmp_matrix[4] + post_cosines[8]*tmp_matrix[8];
	new_matrix[9]= post_cosines[6]*tmp_matrix[1] + post_cosines[7]*tmp_matrix[5] + post_cosines[8]*tmp_matrix[9];
	new_matrix[10]= post_cosines[6]*tmp_matrix[2] + post_cosines[7]*tmp_matrix[6] + post_cosines[8]*tmp_matrix[10];
	new_matrix[11]= post_cosines[6]*tmp_matrix[3] + post_cosines[7]*tmp_matrix[7] + post_cosines[8]*tmp_matrix[11];

	return new_matrix;
    }


    public static final float[] invertMatrix(float[] a) {
	if (DEBUG_INVERSE) System.out.println("\nComputing inverse..\n\tInput is:\t"+Util.arrayToString( a));
	float a_12= 0f;
	float a_13= 0f;
	float a_14= 0f;
	float a_15= 1f;
	float[] b = new float[MATRIX_SIZE_1D];

	double detA= determinant(a);
	if (DEBUG_INVERSE) System.out.println("\tDeterminant is: "+detA);
	if (detA == 0) {
		System.out.println("Warning determinant of transform matrix is zero. Unable to compute inverse.");
		return null;
	}
	//b_11=a_22*a_33*a_44  + a_23*a_34*a_42   + a_24*a_32*a_43  - a_22*a_34*a_43   - a_23*a_32*a_44  - a_24*a_33*a_42
	//b_12=a_12*a_34*a_43  + a_13*a_32*a_44   + a_14*a_33*a_42  - a_12*a_33*a_44   - a_13*a_34*a_42  - a_14*a_32*a_43
	//b_13=a_12*a_23*a_44  + a_13*a_24*a_42   + a_14*a_22*a_43  - a_12*a_24*a_43   - a_13*a_22*a_44  - a_14*a_23*a_42
	//b_14=a_12*a_24*a_33  + a_13*a_22*a_34   + a_14*a_23*a_32  - a_12*a_23*a_34   - a_13*a_24*a_32  - a_14*a_22*a_33
	//b_21=a_21*a_34*a_43  + a_23*a_31*a_44   + a_24*a_33*a_41  - a_21*a_33*a_44   - a_23*a_34*a_41  - a_24*a_31*a_43
	//b_22=a_11*a_33*a_44  + a_13*a_34*a_41   + a_14*a_31*a_43  - a_11*a_34*a_43   - a_13*a_31*a_44  - a_14*a_33*a_41
	//b_23=a_11*a_24*a_43  + a_13*a_21*a_44   + a_14*a_23*a_41  - a_11*a_23*a_44   - a_13*a_24*a_41  - a_14*a_21*a_43
	//b_24=a_11*a_23*a_34  + a_13*a_24*a_31   + a_14*a_21*a_33  - a_11*a_24*a_33   - a_13*a_21*a_34  - a_14*a_23*a_31
	//b_31=a_21*a_32*a_44  + a_22*a_34*a_41   + a_24*a_31*a_42  - a_21*a_34*a_42   - a_22*a_31*a_44  - a_24*a_32*a_41
	//b_32=a_11*a_34*a_42  + a_12*a_31*a_44   + a_14*a_32*a_41  - a_11*a_32*a_44   - a_12*a_34*a_41  - a_14*a_31*a_42
	//b_33=a_11*a_22*a_44  + a_12*a_24*a_41   + a_14*a_21*a_42  - a_11*a_24*a_42   - a_12*a_21*a_44  - a_14*a_22*a_41
	//b_34=a_11*a_24*a_32  + a_12*a_21*a_34   + a_14*a_22*a_31  - a_11*a_22*a_34   - a_12*a_24*a_31  - a_14*a_21*a_32
	//b_41=a_21*a_33*a_42  + a_22*a_31*a_43   + a_23*a_32*a_41  - a_21*a_32*a_43   - a_22*a_33*a_41  - a_23*a_31*a_42
	//b_42=a_11*a_32*a_43  + a_12*a_33*a_41   + a_13*a_31*a_42  - a_11*a_33*a_42   - a_12*a_31*a_43  - a_13*a_32*a_41
	//b_43=a_11*a_23*a_42  + a_12*a_21*a_43   + a_13*a_22*a_41  - a_11*a_22*a_43   - a_12*a_23*a_41  - a_13*a_21*a_42
	//b_44=a_11*a_22*a_33  + a_12*a_23*a_31   + a_13*a_21*a_32  - a_11*a_23*a_32   - a_12*a_21*a_33  - a_13*a_22*a_31
	b[0]= (float)((a[5]*a[10]*a_15 + a[6]*a[11]*a_13 + a[7]*a[9]*a_14 - a[5]*a[11]*a_14 - a[6]*a[9]*a_15 - a[7]*a[10]*a_13)/detA);
	b[1]= (float)((a[1]*a[11]*a_14 + a[2]*a[9]*a_15 + a[3]*a[10]*a_13 - a[1]*a[10]*a_15 - a[2]*a[11]*a_13 - a[3]*a[9]*a_14)/detA);
	b[2]= (float)((a[1]*a[6]*a_15 + a[2]*a[7]*a_13 + a[3]*a[5]*a_14 - a[1]*a[7]*a_14 - a[2]*a[5]*a_15 - a[3]*a[6]*a_13)/detA);
	b[3]= (float)((a[1]*a[7]*a[10] + a[2]*a[5]*a[11] + a[3]*a[6]*a[9] - a[1]*a[6]*a[11] - a[2]*a[7]*a[9] - a[3]*a[5]*a[10])/detA);
	b[4]= (float)((a[4]*a[11]*a_14 + a[6]*a[8]*a_15 + a[7]*a[10]*a_12 - a[4]*a[10]*a_15 - a[6]*a[11]*a_12 - a[7]*a[8]*a_14)/detA);
	b[5]= (float)((a[0]*a[10]*a_15 + a[2]*a[11]*a_12 + a[3]*a[8]*a_14 - a[0]*a[11]*a_14 - a[2]*a[8]*a_15 - a[3]*a[10]*a_12)/detA);
	b[6]= (float)((a[0]*a[7]*a_14 + a[2]*a[4]*a_15 + a[3]*a[6]*a_12 - a[0]*a[6]*a_15 - a[2]*a[7]*a_12 - a[3]*a[4]*a_14)/detA);
	b[7]= (float)((a[0]*a[6]*a[11] + a[2]*a[7]*a[8] + a[3]*a[4]*a[10] - a[0]*a[7]*a[10] - a[2]*a[4]*a[11] - a[3]*a[6]*a[8])/detA);
	b[8]= (float)((a[4]*a[9]*a_15 + a[5]*a[11]*a_12 + a[7]*a[8]*a_13 - a[4]*a[11]*a_13 - a[5]*a[8]*a_15 - a[7]*a[9]*a_12)/detA);
	b[9]= (float)((a[0]*a[11]*a_13 + a[1]*a[8]*a_15 + a[3]*a[9]*a_12 - a[0]*a[9]*a_15 - a[1]*a[11]*a_12 - a[3]*a[8]*a_13)/detA);
	b[10]= (float)((a[0]*a[5]*a_15 + a[1]*a[7]*a_12 + a[3]*a[4]*a_13 - a[0]*a[7]*a_13 - a[1]*a[4]*a_15 - a[3]*a[5]*a_12)/detA);
	b[11]= (float)((a[0]*a[7]*a[9] + a[1]*a[4]*a[11] + a[3]*a[5]*a[8] - a[0]*a[5]*a[11] - a[1]*a[7]*a[8] - a[3]*a[4]*a[9])/detA);
	float b_12= (float)((a[4]*a[10]*a_13 + a[5]*a[8]*a_14 + a[6]*a[9]*a_12 - a[4]*a[9]*a_14 - a[5]*a[10]*a_12 - a[6]*a[8]*a_13)/detA);
	float b_13= (float)((a[0]*a[9]*a_14 + a[1]*a[10]*a_12 + a[2]*a[8]*a_13 - a[0]*a[10]*a_13 - a[1]*a[8]*a_14 - a[2]*a[9]*a_12)/detA);
	float b_14= (float)((a[0]*a[6]*a_13 + a[1]*a[4]*a_14 + a[2]*a[5]*a_12 - a[0]*a[5]*a_14 - a[1]*a[6]*a_12 - a[2]*a[4]*a_13)/detA);
	float b_15= (float)((a[0]*a[5]*a[10] + a[1]*a[6]*a[8] + a[2]*a[4]*a[9] - a[0]*a[6]*a[9] - a[1]*a[4]*a[10] - a[2]*a[5]*a[8])/detA);

	if ( !floatEquals(b_12,0f) || !floatEquals(b_13,0) || !floatEquals(b_14,0) || !floatEquals(b_15,1) ) {
		System.out.println("Warning! Problem calculating the inverse transform!");
		System.out.println("["+b_12+" "+b_13+" "+b_14+" "+b_15+"]");
		if (DEBUG_INVERSE) System.out.println("\tCalculated:\t"+Util.arrayToString( b));
	}

	return b;
    }

    public static final boolean floatEquals(float a, float b){
	if(Math.abs(a - b) < EPSILON) return true;
	else return false;
    }



    private static final double determinant(float[] a) {
	double det;
	float a_12= 0f;
	float a_13= 0f;
	float a_14= 0f;
	float a_15= 1f;

	//det=  a_11*a_22*a_33*a_44 + a_11*a_23*a_34*a_42 + a_11*a_24*a_32*a_43 +
	//      a_12*a_21*a_34*a_43 + a_12*a_23*a_31*a_44 + a_12*a_24*a_33*a_41 +
	//      a_13*a_21*a_32*a_44 + a_13*a_22*a_34*a_41 + a_13*a_24*a_31*a_42 +
	//      a_14*a_21*a_33*a_42 + a_14*a_22*a_31*a_43 + a_14*a_23*a_32*a_41 -
	//      a_11*a_22*a_34*a_43 - a_11*a_23*a_32*a_44 - a_11*a_24*a_33*a_42 -
	//      a_12*a_21*a_33*a_44 - a_12*a_23*a_34*a_41 - a_12*a_24*a_31*a_43 -
	//      a_13*a_21*a_34*a_42 - a_13*a_22*a_31*a_44 - a_13*a_24*a_32*a_41 -
	//      a_14*a_21*a_32*a_43 - a_14*a_22*a_33*a_41 - a_14*a_23*a_31*a_42;
	det= a[0]*a[5]*a[10]*a_15 + a[0]*a[6]*a[11]*a_13 + a[0]*a[7]*a[9]*a_14 +
		a[1]*a[4]*a[11]*a_14 + a[1]*a[6]*a[8]*a_15 + a[1]*a[7]*a[10]*a_12 +
		a[2]*a[4]*a[9]*a_15 + a[2]*a[5]*a[11]*a_12 + a[2]*a[7]*a[8]*a_13 +
		a[3]*a[4]*a[10]*a_13 + a[3]*a[5]*a[8]*a_14 + a[3]*a[6]*a[9]*a_12 -
		a[0]*a[5]*a[11]*a_14 - a[0]*a[6]*a[9]*a_15 - a[0]*a[7]*a[10]*a_13 -
		a[1]*a[4]*a[10]*a_15 - a[1]*a[6]*a[11]*a_12 - a[1]*a[7]*a[8]*a_14 -
		a[2]*a[4]*a[11]*a_13 - a[2]*a[5]*a[8]*a_15 - a[2]*a[7]*a[9]*a_12 -
		a[3]*a[4]*a[9]*a_14 - a[3]*a[5]*a[10]*a_12 - a[3]*a[6]*a[8]*a_13;

	return det;
    }


    public static final float[][] invertMatrix(float[][] a) {
	if (DEBUG_INVERSE) System.out.println("\nComputing 2D inverse..\n\tInput is:\t"+Util.arrayToString( a));
	float a_30= 0f;
	float a_31= 0f;
	float a_32= 0f;
	float a_33= 1f;
	float[][] b = new float [MATRIX_SIZE_2D_1][MATRIX_SIZE_2D_2];

	double detA= determinant(a);
	if (DEBUG_INVERSE) System.out.println("\tDeterminant is: "+detA);
	if (detA == 0) {
		System.out.println("Warning determinant of transform matrix is zero. Unable to compute inverse.");
		return null;
	}
	b[0][0]=(float)((a[1][1]*a[2][2]*a_33       + a[1][2]*a[2][3]*a_31    + a[1][3]*a[2][1]*a_32 -
	        	a[1][1]*a[2][3]*a_32       - a[1][2]*a[2][1]*a_33    - a[1][3]*a[2][2]*a_31)/detA);
	b[0][1]=(float)((a[0][1]*a[2][3]*a_32       + a[0][2]*a[2][1]*a_33    + a[0][3]*a[2][2]*a_31 -
	        	a[0][1]*a[2][2]*a_33       - a[0][2]*a[2][3]*a_31    - a[0][3]*a[2][1]*a_32)/detA);
	b[0][2]=(float)((a[0][1]*a[1][2]*a_33       + a[0][2]*a[1][3]*a_31    + a[0][3]*a[1][1]*a_32 -
	        	a[0][1]*a[1][3]*a_32       - a[0][2]*a[1][1]*a_33    - a[0][3]*a[1][2]*a_31)/detA);
	b[0][3]=(float)((a[0][1]*a[1][3]*a[2][2]    + a[0][2]*a[1][1]*a[2][3] + a[0][3]*a[1][2]*a[2][1] -
	        	a[0][1]*a[1][2]*a[2][3]    - a[0][2]*a[1][3]*a[2][1] - a[0][3]*a[1][1]*a[2][2])/detA);
	b[1][0]=(float)((a[1][0]*a[2][3]*a_32       + a[1][2]*a[2][0]*a_33    + a[1][3]*a[2][2]*a_30 -
	        	a[1][0]*a[2][2]*a_33       - a[1][2]*a[2][3]*a_30    - a[1][3]*a[2][0]*a_32)/detA);
	b[1][1]=(float)((a[0][0]*a[2][2]*a_33       + a[0][2]*a[2][3]*a_30    + a[0][3]*a[2][0]*a_32 -
	        	a[0][0]*a[2][3]*a_32       - a[0][2]*a[2][0]*a_33    - a[0][3]*a[2][2]*a_30)/detA);
	b[1][2]=(float)((a[0][0]*a[1][3]*a_32       + a[0][2]*a[1][0]*a_33    + a[0][3]*a[1][2]*a_30 -
	        	a[0][0]*a[1][2]*a_33       - a[0][2]*a[1][3]*a_30    - a[0][3]*a[1][0]*a_32)/detA);
	b[1][3]=(float)((a[0][0]*a[1][2]*a[2][3]    + a[0][2]*a[1][3]*a[2][0] + a[0][3]*a[1][0]*a[2][2] -
	        	a[0][0]*a[1][3]*a[2][2]    - a[0][2]*a[1][0]*a[2][3] - a[0][3]*a[1][2]*a[2][0])/detA);
	b[2][0]=(float)((a[1][0]*a[2][1]*a_33       + a[1][1]*a[2][3]*a_30    + a[1][3]*a[2][0]*a_31 -
	        	a[1][0]*a[2][3]*a_31       - a[1][1]*a[2][0]*a_33    - a[1][3]*a[2][1]*a_30)/detA);
	b[2][1]=(float)((a[0][0]*a[2][3]*a_31       + a[0][1]*a[2][0]*a_33    + a[0][3]*a[2][1]*a_30 -
	        	a[0][0]*a[2][1]*a_33       - a[0][1]*a[2][3]*a_30    - a[0][3]*a[2][0]*a_31)/detA);
	b[2][2]=(float)((a[0][0]*a[1][1]*a_33       + a[0][1]*a[1][3]*a_30    + a[0][3]*a[1][0]*a_31 -
	        	a[0][0]*a[1][3]*a_31       - a[0][1]*a[1][0]*a_33    - a[0][3]*a[1][1]*a_30)/detA);
	b[2][3]=(float)((a[0][0]*a[1][3]*a[2][1]    + a[0][1]*a[1][0]*a[2][3] + a[0][3]*a[1][1]*a[2][0] -
	        	a[0][0]*a[1][1]*a[2][3]    - a[0][1]*a[1][3]*a[2][0] - a[0][3]*a[1][0]*a[2][1])/detA);
	float b_30=(float)((a[1][0]*a[2][2]*a_31    + a[1][1]*a[2][0]*a_32    + a[1][2]*a[2][1]*a_30 -
	        	a[1][0]*a[2][1]*a_32       - a[1][1]*a[2][2]*a_30    - a[1][2]*a[2][0]*a_31)/detA);
	float b_31=(float)((a[0][0]*a[2][1]*a_32    + a[0][1]*a[2][2]*a_30    + a[0][2]*a[2][0]*a_31 -
	        	a[0][0]*a[2][2]*a_31       - a[0][1]*a[2][0]*a_32    - a[0][2]*a[2][1]*a_30)/detA);
	float b_32=(float)((a[0][0]*a[1][2]*a_31    + a[0][1]*a[1][0]*a_32    + a[0][2]*a[1][1]*a_30 -
	        	a[0][0]*a[1][1]*a_32       - a[0][1]*a[1][2]*a_30    - a[0][2]*a[1][0]*a_31)/detA);
	float b_33=(float)((a[0][0]*a[1][1]*a[2][2] + a[0][1]*a[1][2]*a[2][0] + a[0][2]*a[1][0]*a[2][1] -
	        	a[0][0]*a[1][2]*a[2][1]    - a[0][1]*a[1][0]*a[2][2] - a[0][2]*a[1][1]*a[2][0])/detA);

	if ( !floatEquals(b_30,0) || !floatEquals(b_31,0) || !floatEquals(b_32,0) || !floatEquals(b_33,1) ) {
		System.out.println("Warning! Problem calculating the inverse transform!");
		System.out.println("["+b_30+" "+b_31+" "+b_32+" "+b_33+"]");
		if (DEBUG_INVERSE) System.out.println("\tCalculated:\t"+Util.arrayToString( b));
	}

	return b;
    }



    private static final double determinant(float[][] a) {
	double det;
	float a_30= 0f;
	float a_31= 0f;
	float a_32= 0f;
	float a_33= 1f;

	det=  a[0][0]*a[1][1]*a[2][2]*a_33 + a[0][0]*a[1][2]*a[2][3]*a_31 + a[0][0]*a[1][3]*a[2][1]*a_32 +
	      a[0][1]*a[1][0]*a[2][3]*a_32 + a[0][1]*a[1][2]*a[2][0]*a_33 + a[0][1]*a[1][3]*a[2][2]*a_30 +
	      a[0][2]*a[1][0]*a[2][1]*a_33 + a[0][2]*a[1][1]*a[2][3]*a_30 + a[0][2]*a[1][3]*a[2][0]*a_31 +
	      a[0][3]*a[1][0]*a[2][2]*a_31 + a[0][3]*a[1][1]*a[2][0]*a_32 + a[0][3]*a[1][2]*a[2][1]*a_30 -
	      a[0][0]*a[1][1]*a[2][3]*a_32 - a[0][0]*a[1][2]*a[2][1]*a_33 - a[0][0]*a[1][3]*a[2][2]*a_31 -
	      a[0][1]*a[1][0]*a[2][2]*a_33 - a[0][1]*a[1][2]*a[2][3]*a_30 - a[0][1]*a[1][3]*a[2][0]*a_32 -
	      a[0][2]*a[1][0]*a[2][3]*a_31 - a[0][2]*a[1][1]*a[2][0]*a_33 - a[0][2]*a[1][3]*a[2][1]*a_30 -
	      a[0][3]*a[1][0]*a[2][1]*a_32 - a[0][3]*a[1][1]*a[2][2]*a_30 - a[0][3]*a[1][2]*a[2][0]*a_31;

	return det;
    }


} // end of class Util
