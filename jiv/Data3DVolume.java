
// $Id: Data3DVolume.java,v 1.2 2001-09-21 16:42:13 cc Exp $
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

import java.net.*;
import java.io.*;
import java.util.zip.*;

/**
 * Loads, stores, and provides access to a 3D image volume.
 *
 * @author Chris Cocosco (crisco@bic.mni.mcgill.ca)
 * @version $Id: Data3DVolume.java,v 1.2 2001-09-21 16:42:13 cc Exp $
 */
public final class Data3DVolume {

    /** for development only: artificially delay the downloads */
    /*private*/ static final boolean DELAY_DOWNLOAD= true;

    public static final int DOWNLOAD_UPFRONT=		1;
    public static final int DOWNLOAD_ON_DEMAND= 	2;
    public static final int DOWNLOAD_HYBRID= 		3;


    /*private*/ byte[][][] 	voxels;     // (z,y,x)!

    /** indicates if that slice number was already downloaded */
    /*private*/ volatile boolean    	t_slice_downloaded[];
    /*private*/ volatile boolean	s_slice_downloaded[];
    /*private*/ volatile boolean	c_slice_downloaded[];
    
    /*private*/ String 		volume_url;	// eg http://www/foo/colin27.raw.gz
    /*private*/ String 		slice_url_base; // eg http://www/foo/colin27
    /*private*/ String 		slice_url_ext;  // eg .raw.gz

    /*private*/ String 		nick_name;


    final public int getXSize() { return 181; }
    final public int getYSize() { return 217; }
    final public int getZSize() { return 181; }


    public Data3DVolume()
    {
	voxels= new byte[ getZSize()][ getYSize()][ getXSize()];

	// initialize it to the dummy pattern
	byte[] 	dummy_line1= new byte[ getXSize()];;
	byte[] 	dummy_line2= new byte[ getXSize()];;
	for( int i= 0; i < getXSize(); ++i) {
	    if( ((i/2) % 2) == 0 ) {
		dummy_line1[ i]= (byte)0;
		dummy_line2[ i]= (byte)255;
	    }
	    else {
		dummy_line1[ i]= (byte)255;
		dummy_line2[ i]= (byte)0;
	    }
	}
	for( int z= 0; z < getZSize(); ++z) {     
	    for( int y= 0; y < getYSize(); ++y) {
		byte[] pat= ( ((y/2) % 2) == 0 ) ? dummy_line1 : dummy_line2 ;
		System.arraycopy( pat, 0, voxels[ z][ y], 0, getXSize());
	    }
	}

	// by default initialized to false (TODO: chk if is it guaranteed!)
	t_slice_downloaded= new boolean[ getZSize()];
	s_slice_downloaded= new boolean[ getXSize()];
	c_slice_downloaded= new boolean[ getYSize()];
    }

    /** Note: it's not _required_ to declare SecurityException (since
     it's a subclass of RuntimeException), but we do it for clarity --
     this error is likely to happen when working with url-s...
    */
    public Data3DVolume( URL source_url) 
	throws IOException, SecurityException
    {
	this( source_url, null, DOWNLOAD_UPFRONT);
    }

    public Data3DVolume( URL source_url, String nick_name) 
	throws IOException, SecurityException
    {
	this( source_url, nick_name, DOWNLOAD_UPFRONT);
    }

    public Data3DVolume( final URL source_url, String nick_name, int download_method) 
	throws IOException, SecurityException
    {
	this();
	
	volume_url= source_url.toString();
	slice_url_ext= Util.getExtension( volume_url);
	if( slice_url_ext.length() > 0 ) {
	    int ext= volume_url.lastIndexOf( slice_url_ext);
	    slice_url_base= volume_url.substring( 0, ext);
	}
	else {
	    slice_url_base= volume_url;
	}
	this.nick_name= (nick_name != null) ? nick_name : "(unnamed)";

	switch( download_method) {

	case DOWNLOAD_UPFRONT :

	    _downloadAllVolume( source_url);
	    break;

	case DOWNLOAD_ON_DEMAND :

	    break;

	case DOWNLOAD_HYBRID :

	    // start the parallel ("bg") download 
	    Thread t= new Thread() {
		    public void run() 
		    {
			try {
			    _downloadAllVolume( source_url);
			}
			catch( Exception e) {
			    System.err.println( e);
			    return;
			}
		    }
		};
	    t.setPriority( Thread.NORM_PRIORITY - 1 ); 
	    t.start();
	    break;

	default:
	    throw new 
		IllegalArgumentException( this + " unknown download method: " +	download_method);
	}
    }

    final private void _downloadAllVolume( URL source_url)
	throws IOException, SecurityException
    {
	InputStream input_stream= null;
	try {
	    input_stream= _openURL( source_url);

	    int read_count, left;
	    // for speed, use a stack variable instead of the instance field:
	    byte[][][] voxels= this.voxels;
	    for( int z= 0; z < getZSize(); ++z) {
		for( int y= 0; y < getYSize(); ++y) {
		    /* the tricky bit here is that this read()
		       sometimes only reads part of the data, so we
		       need to call it again and again until we get
		       all our data (btw, this is _not_ documented
		       anywhere!) */
		    left= getXSize();
		    while( left > 0) {
			if( DELAY_DOWNLOAD ) Util.sleep( 7); 
			
			read_count= input_stream.read( voxels[ z][ y], 
						       getXSize()-left, left);
			if( read_count <= 0)
			    throw new IOException( source_url.toString()
						   + " : premature end of data : "
						   + z + " " + y + " : " + read_count);
			left -= read_count;
		    }
		}
		t_slice_downloaded[ z]= true;
	    }
	    System.out.println( source_url + " loading done!");
	}
	finally {
	    if( input_stream != null) {

		// TODO: what if we try to close() a stream that wasn't 
		// successfully opened? is this a problem?
		// TODO: does the GZIPInputStream also close the 
		// url_connection's InputStream? Here we assume that it does...
		input_stream.close();
		input_stream= null;
	    }
	}
	int i;
	for( i= 0; i < getXSize(); ++i)
	    s_slice_downloaded[ i]= true;
	for( i= 0; i < getYSize(); ++i)
	    c_slice_downloaded[ i]= true;
    }

    final private void _downloadSlice( URL source_url, 
				       byte[] slice, 
				       final int slice_width,
				       SliceImageProducer consumer, 
				       int slice_number )
	throws IOException, SecurityException
    {
	InputStream input_stream= null;
	try {
	    input_stream= _openURL( source_url);

	    // store the slice in "display order" (vertically flipped)
	    int read_count, left, offset;
	    for( int l= slice.length/slice_width - 1; l >= 0; --l) {
		left= slice_width;
		offset= l * slice_width;
		while( left > 0) {
		    read_count= input_stream.read( slice, 
						   offset + slice_width - left, 
						   left);
		    if( read_count <= 0)
			throw new IOException( source_url.toString()
					       + " : premature end of data : "
					       + l + " : " + read_count);
		    left -= read_count;
		}
	    }
	    System.out.println( source_url + " loading done!");
	}
	finally {
	    if( input_stream != null) {
		input_stream.close();
		input_stream= null;
	    }
	}

	if( consumer != null ) 
	    consumer.sliceDataUpdated( slice, slice_number);
    }

    final private InputStream _openURL( URL source_url) 
	throws IOException, SecurityException 
    {
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


    final public String getNickName() { return nick_name; }

    final public byte getVoxel( int x, int y, int z)
    {
	return voxels[ z][ y][ x];
    }

    final public byte getVoxel( Point3Dint voxel)
    {
	return getVoxel( voxel.x, voxel.y, voxel.z);
    }

    final public int getVoxelAsInt( int x, int y, int z)
    {
	// NB: this '&' needs to be done if you want to get 255
	// for the maximum valued voxel! otherwise you'll get 
	// -1 because byte-s are signed in Java!
	return 0xFF & getVoxel( x, y, z);
    }

    final public int getVoxelAsInt( Point3Dint voxel)
    {
	return getVoxelAsInt( voxel.x, voxel.y, voxel.z);
    }

    /** the result is arranged in "display order" (i.e. origin in _top_ left corner),
	or in other words "flipped" -- hence, it can be then directly fed to an 
	image producer mechanism. 
    */
    final public byte[] getTransverseSlice( final int z )
    {
	byte[] slice= new byte[ getYSize() * getXSize()];
	getTransverseSlice( z, slice, null);
	return slice;
    }

    /** this overloaded version is easier on the heap & garbage collector because
	it encourages reuse of an already allocated 'slice' array (which has
	to be large enough to hold the result, naturally).

	<em>Note:</em> param 'slice' should not be used outside the 
	current thread!!
    */
    final public void getTransverseSlice( final int z, byte[] slice,
					  final SliceImageProducer consumer )
    {
	// for speed, use a stack variable instead of the instance field:
	final byte[][][] voxels= this.voxels;
	final boolean slice_downloaded= t_slice_downloaded[ z];

	// NB: this vertically flips the image...
	// (decreasing index loops are rumored to run faster in Java)
	for( int y= getYSize()-1, offset= 0; y >= 0; --y, offset += getXSize())
	    System.arraycopy( voxels[ z][ y], 0, slice, offset, getXSize());
	
	if( consumer != null )
	    consumer.sliceDataUpdated( slice, z);

	if( slice_downloaded )
	    return;

	// download the slice in a parallel (bg) thread
	Thread t= new Thread() {
		public void run() 
		{ 
		    byte[] buff= new byte[getXSize()*getYSize()];
		    try { 
			if( DELAY_DOWNLOAD ) Util.sleep( 3000); 
			_downloadSlice( new URL( slice_url_base + "/t/" + z + 
						 slice_url_ext ),
					buff,
					getXSize(),
					consumer, 
					z );
		    }
		    catch( Exception e) {
			System.err.println( e);
			return;
		    }

		    // the rest should be synchronized( Data3DVolume.this)
		    // but nothing bad will happen if two parallel updates occur
		    // (the info being written is identical)
		    // TODO: verify this!!!

		    for( int y= getYSize()-1, offset= 0; y >= 0; --y, offset += getXSize())
			System.arraycopy( buff, offset, voxels[ z][ y], 0, getXSize());
		    t_slice_downloaded[ z]= true;

		    // TODO? chk if all transverse slices were downloaded,
		    // and then set all c_slice & s_slice to true also ...
		}
	    };
	t.start();
    }

    final public byte[] getSagittalSlice( final int x )
    {
	byte[] slice= new byte[ getZSize() * getYSize()];
	getSagittalSlice( x, slice, null);
	return slice;
    }

    final public void getSagittalSlice( final int x, byte[] slice,
					final SliceImageProducer consumer)
    {
	// for speed, use a stack variable instead of the instance field:
	final byte[][][] voxels= this.voxels;
	final boolean slice_downloaded= s_slice_downloaded[ x];

	// NB: this vertically flips the image...
	for( int z= getZSize()-1, offset= 0; z >= 0; --z, offset += getYSize()) 
	    for( int y= getYSize()-1; y >= 0; --y)
		slice[ offset + y]= voxels[ z][ y][ x];

	if( consumer != null )
	    consumer.sliceDataUpdated( slice, x);

	if( slice_downloaded )
	    return;

	// download the slice in a parallel (bg) thread
	Thread t= new Thread() {
		public void run() 
		{ 
		    byte[] buff= new byte[getZSize()*getYSize()];
		    try { 
			if( DELAY_DOWNLOAD ) Util.sleep( 3000); 
			_downloadSlice( new URL( slice_url_base + "/s/" + x + 
						 slice_url_ext ),
					buff,
					getYSize(),
					consumer, 
					x );
		    }
		    catch( Exception e) {
			System.err.println( e);
			return;
		    }
		    for( int z= getZSize()-1, offset= 0; z >= 0; --z, offset += getYSize()) 
			for( int y= getYSize()-1; y >= 0; --y)
			    voxels[ z][ y][ x]= buff[ offset + y]; 
		    s_slice_downloaded[ x]= true;
		}
	    };
	t.start();
    }

    final public byte[] getCoronalSlice( final int y )
    {
	byte[] slice= new byte[ getZSize() * getXSize()];
	getCoronalSlice( y, slice, null);
	return slice;
    }

    final public void getCoronalSlice( final int y, byte[] slice,
				       final SliceImageProducer consumer)
    {
	// for speed, use a stack variable instead of the instance field:
	final byte[][][] voxels= this.voxels;
	final boolean slice_downloaded= c_slice_downloaded[ y];

	// NB: this vertically flips the image...
	for( int z= getZSize()-1, offset= 0; z >= 0; --z, offset += getXSize())
	    System.arraycopy( voxels[ z][ y], 0, slice, offset, getXSize());

	if( consumer != null )
	    consumer.sliceDataUpdated( slice, y);

	if( slice_downloaded )
	    return;

	// download the slice in a parallel (bg) thread
	Thread t= new Thread() {
		public void run() 
		{ 
		    byte[] buff= new byte[getZSize()*getXSize()];
		    try { 
			if( DELAY_DOWNLOAD ) Util.sleep( 3000); 
			_downloadSlice( new URL( slice_url_base + "/c/" + y + 
						 slice_url_ext ),
					buff,
					getXSize(),
					consumer, 
					y );
		    }
		    catch( Exception e) {
			System.err.println( e);
			return;
		    }
		    for( int z= getZSize()-1, offset= 0; z >= 0; --z, offset += getXSize())
			System.arraycopy( buff, offset, voxels[ z][ y], 0, getXSize());
		    c_slice_downloaded[ y]= true;
		}
	    };
	t.start();
    }

    // debugging aid...
    public void printTrueRange() 
    {
	int min= voxels[ 0][ 0][ 0];
	int max= voxels[ 0][ 0][ 0];
	for( int z= 0; z < getZSize(); ++z)
	    for( int y= 0; y < getYSize(); ++y)
		for( int x= 0; x < getXSize(); ++x) {
		    int vox= getVoxelAsInt( x, y, z);
		    // NB: if you want to assign 255 to a byte, you need to
		    // do assign it '(byte)255' (which does the right thing)
		    if( vox < min)
			min= vox;
		    else if( vox > max)
			max= vox;
		}
	System.out.println( "true range: " + min + " " + max);
    }
}

