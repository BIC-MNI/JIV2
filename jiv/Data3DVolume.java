
// $Id: Data3DVolume.java,v 1.1 2001-04-08 00:04:27 cc Exp $
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
 * @version $Id: Data3DVolume.java,v 1.1 2001-04-08 00:04:27 cc Exp $
 */
public final class Data3DVolume {

    /*private*/ byte[][][] voxels; 			// (z,y,x)!
    /*private*/ URL source_url;
    /*private*/ String nick_name;

    final public int getXSize() { return 181; }
    final public int getYSize() { return 217; }
    final public int getZSize() { return 181; }

    /** currently, this constructor is practically useless... */
    public Data3DVolume()
    {
	voxels= new byte[ getZSize()][ getYSize()][ getXSize()];
    }

    /** Note: it's not _required_ to declare SecurityException (since
     it's a subclass of RuntimeException), but we do it for clarity --
     this error is likely to happen when working with url-s...
    */
    public Data3DVolume( URL source_url) 
	throws IOException, SecurityException
    {
	this( source_url, null);
    }

    public Data3DVolume( URL source_url, String nick_name) 
	throws IOException, SecurityException
    {
	this();
	
	this.source_url= source_url;
	this.nick_name= (nick_name != null) ? nick_name : "(unnamed)";
	InputStream input_stream= null;
	try {
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
			read_count= input_stream.read( voxels[ z][ y], 
						       getXSize()-left, left);
			if( read_count <= 0)
			    throw new IOException( source_url.toString()
						   + " : premature end of data : "
						   + z + " " + y + " : " + read_count);
			left -= read_count;
		    }
		}
	    }
	    // TODO: I once got a "IOException: server status 206" when loading
	    // the applet via http ... but couldn't reproduce it ever since.
	    // what went wrong?
	    // (btw, 206 == HTTP_PARTIAL : 
	    // "HTTP response code that means the partial request has been fulfilled") 

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
	getTransverseSlice( z, slice);
	return slice;
    }

    /** this overloaded version is easier on the heap & garbage collector because
	it encourages reuse of an already allocated 'slice' array (which has
	to be large enough to hold the result, naturally)
    */
    final public void getTransverseSlice( final int z, byte[] slice)
    {
	// for speed, use a stack variable instead of the instance field:
	final byte[][][] voxels= this.voxels;

	// decreasing index loops are rumored to run faster in Java
	for( int y= getYSize()-1, offset= 0; y >= 0; --y, offset += getXSize())
	    System.arraycopy( voxels[ z][ y], 0, slice, offset, getXSize());
    }

    final public byte[] getSagittalSlice( final int x )
    {
	byte[] slice= new byte[ getZSize() * getYSize()];
	getSagittalSlice( x, slice);
	return slice;
    }

    final public void getSagittalSlice( final int x, byte[] slice)
    {
	// for speed, use a stack variable instead of the instance field:
	final byte[][][] voxels= this.voxels;

	for( int z= getZSize()-1, offset= 0; z >= 0; --z, offset += getYSize()) 
	    for( int y= getYSize()-1; y >= 0; --y)
		slice[ offset + y]= voxels[ z][ y][ x];
    }

    final public byte[] getCoronalSlice( final int y )
    {
	byte[] slice= new byte[ getZSize() * getXSize()];
	getCoronalSlice( y, slice);
	return slice;
    }

    final public void getCoronalSlice( final int y, byte[] slice)
    {
	// for speed, use a stack variable instead of the instance field:
	final byte[][][] voxels= this.voxels;

	for( int z= getZSize()-1, offset= 0; z >= 0; --z, offset += getXSize())
	    System.arraycopy( voxels[ z][ y], 0, slice, offset, getXSize());
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

