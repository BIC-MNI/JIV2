
// $Id: Data3DVolume.java,v 1.5 2001-10-02 01:27:09 cc Exp $
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

/*
  WISH LIST:

 1. implement _downloadAllVolumeBySlice() : download all t slices,
 starting with the "middle" ones (most useful!), and checking 
 beforehand for each slice if not already done.
 

 2. have 3 states for {t,s,c}_slice_downloaded : 
 no, in_progress (some thread working on it already), done .
 
       -> this will eliminate useless duplicate downloads
       (eg when using merged panels, and sync mode)
       -> the new thread can wait() till state becomes 'done'...
*/

/**
 * Loads, stores, and provides access to a 3D image volume.
 *
 * @author Chris Cocosco (crisco@bic.mni.mcgill.ca)
 * @version $Id: Data3DVolume.java,v 1.5 2001-10-02 01:27:09 cc Exp $
 */
public final class Data3DVolume {

    /*private*/ static final boolean 	DEBUG= false;

    /** for development only: artificially delay the downloads */
    /*private*/ static final boolean DELAY_DOWNLOAD= false;

    public static final int DOWNLOAD_UPFRONT=		1;
    public static final int DOWNLOAD_ON_DEMAND= 	2;
    public static final int DOWNLOAD_HYBRID= 		3;


    /*private*/ byte[][][] 	voxels;     // (z,y,x)!

    /*private*/ VolumeHeader.ResampleTable	resample;	

    /** Indicates if that slice number was already downloaded. Slice
        numbers are for the input (volume_header), not for the
        internal (common_sampling) representation! */
    /*private*/ volatile boolean[]    	t_slice_downloaded;
    /*private*/ volatile boolean[]	s_slice_downloaded;
    /*private*/ volatile boolean[]	c_slice_downloaded;
    /** convenience pointer to <code>{t,s,c}_slice_downloaded</code>,
        indexed by canonical dimension, ie slice_downloaded= {
        s_slice_downloaded, c_slice_downloaded, t_slice_downloaded } */
    /*private*/ volatile boolean[][]	slice_downloaded;

    /** Should be set when all volume data has been downloaded --
        which means that all elements of
        <code>{t,s,c}_slice_downloaded</code> are true */
    /*private*/ boolean			all_data_downloaded;

    /*private*/ String 		volume_url;	// eg http://www/foo/colin27.raw.gz
    /*private*/ String 		slice_url_base; // eg http://www/foo/colin27
    /*private*/ String 		slice_url_ext;  // eg .raw.gz

    /*private*/ String 		nick_name;

    /** header associated with the data in volume_url */
    /*private*/ VolumeHeader	volume_header;
    /*private*/ VolumeHeader	common_sampling;


    public Data3DVolume( VolumeHeader common_sampling)
    {
	this.common_sampling= common_sampling;
	voxels= new byte[ getZSize()][ getYSize()][ getXSize()];
    }

    /** Note: it's not _required_ to declare SecurityException (since
     it's a subclass of RuntimeException), but we do it for clarity --
     this error is likely to happen when working with url-s...
    */
    public Data3DVolume( VolumeHeader common_sampling, final URL source_url, VolumeHeader volume_header, String nick_name, int download_method) 
	throws IOException, SecurityException
    {
	this( common_sampling);

	this.volume_header= volume_header;
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

	resample= volume_header.getResampleTable( common_sampling);

	/* initialize this volume's region to the dummy pattern
	   ("solid color", more exactly), but only within the extend
	   of this file -- leave black padding outside... */
	{
	    int[][] 	map_start= this.resample.start;
	    int[][] 	map_end= this.resample.end;
	    int start_x, end_x, start_y, end_y, start_z, end_z;
	    start_x= Math.min( map_start[0][0], _last( map_start[0]));
	    end_x= Math.max( map_end[0][0], _last( map_end[0]));
	    start_y= Math.min( map_start[1][0], _last( map_start[1]));
	    end_y= Math.max( map_end[1][0], _last( map_end[1]));
	    start_z= Math.min( map_start[2][0], _last( map_start[2]));
	    end_z= Math.max( map_end[2][0], _last( map_end[2]));

	    int 	line_len= end_x - start_x + 1;
	    byte[] 	dummy_line1= new byte[ line_len];
	    byte 	dummy_val= (byte)(255*0.2);
	    for( int i= 0; i < line_len; ++i) 
		dummy_line1[ i]= dummy_val;

	    for( int z= start_z; z <= end_z; ++z)
		for( int y= start_y; y <= end_y ; ++y)
		    System.arraycopy( dummy_line1, 0, 
				      voxels[ z][ y], start_x,
				      line_len);
	}

	// by default initialized to false (guaranteed by the Java spec)
	t_slice_downloaded= new boolean[ volume_header.getSizeZ()];
	s_slice_downloaded= new boolean[ volume_header.getSizeX()];
	c_slice_downloaded= new boolean[ volume_header.getSizeY()];
	slice_downloaded= new boolean[][] { 
	    s_slice_downloaded, c_slice_downloaded, t_slice_downloaded 
	};

	/* data downloading */
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

    final public int getXSize() { return common_sampling.getSizeX(); }

    final public int getYSize() { return common_sampling.getSizeY(); }

    final public int getZSize() { return common_sampling.getSizeZ(); }

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


    static /*private*/ int[]		std_dim_order;
    static /*private*/ int[] 		std_dim_perm;
    static {
	std_dim_perm=  std_dim_order= new int[] { 2, 1, 0 };
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
	boolean already_downloaded;

	/* TODO: you can use if( all_data_downloaded ) to speed this
           up for the common situation... */

	/* grossly inefficient... (TODO: use pre-allocated
	   fields... -- this won't be thread-safe, but is it an
	   issue??? do I ever have multiple threads calling this
	   method??) -- Yes (callback to sliceDataUpdated from the
	   slice download threads) */
	Point3Dfloat world= common_sampling.voxel2world( 0, 0, z);
	final Point3Dint file_voxel= volume_header.world2voxel( world);

	if( file_voxel.z < 0 || file_voxel.z >= t_slice_downloaded.length)
	    already_downloaded= true;
	else
	    already_downloaded= t_slice_downloaded[ file_voxel.z];

	final int x_size= getXSize();
	final int y_size= getYSize();
	byte[][][] voxels= this.voxels;
	// NB: this vertically flips the image...
	// (decreasing index loops are rumored to run faster in Java)
	for( int y= y_size - 1, offset= 0; y >= 0; --y, offset += x_size)
	    System.arraycopy( voxels[ z][ y], 0, slice, offset, x_size);
	
	if( consumer != null )
	    consumer.sliceDataUpdated( z);

	if( already_downloaded )
	    return;

	// download the slice in a parallel (bg) thread
	Thread t= new Thread() {
		public void run() 
		{ 
		    byte[] buff= 
			new byte[ volume_header.getSizeX() * volume_header.getSizeY()];

		    try { 
			_downloadSlice( new URL( slice_url_base + "/t/" + 
						 file_voxel.z + slice_url_ext ),
					buff,
					volume_header.getSizeX(),
					consumer, 
					file_voxel.z );
		    }
		    catch( Exception e) {
			System.err.println( e);
			return;
		    }

		    // the rest should be synchronized( Data3DVolume.this)
		    // but nothing bad will happen if two parallel updates occur
		    // (the info being written is identical)
		    // TODO: verify this!!!

		    _saveSlab( buff, 
			       std_dim_order, std_dim_perm, 
			       new int[] { file_voxel.z, 0, 0}, 
			       new int[] { 1, volume_header.getSizeY(), volume_header.getSizeX()});
		    t_slice_downloaded[ file_voxel.z]= true;

		    if( consumer != null )
			consumer.sliceDataUpdated( z);
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
	boolean already_downloaded;
	Point3Dfloat world= common_sampling.voxel2world( x, 0, 0);
	final Point3Dint file_voxel= volume_header.world2voxel( world);

	if( file_voxel.x < 0 || file_voxel.x >= s_slice_downloaded.length)
	    already_downloaded= true;
	else
	    already_downloaded= s_slice_downloaded[ file_voxel.x];

	final int y_size= getYSize();
	final int z_size= getZSize();
	byte[][][] voxels= this.voxels;
	// NB: this vertically flips the image...
	for( int z= z_size - 1, offset= 0; z >= 0; --z, offset += y_size) 
	    for( int y= y_size - 1; y >= 0; --y)
		slice[ offset + y]= voxels[ z][ y][ x];

	if( consumer != null )
	    consumer.sliceDataUpdated( x);

	if( already_downloaded )
	    return;

	// download the slice in a parallel (bg) thread
	Thread t= new Thread() {
		public void run() 
		{ 
		    byte[] buff= 
			new byte[ volume_header.getSizeZ() * volume_header.getSizeY()];

		    try { 
			_downloadSlice( new URL( slice_url_base + "/s/" + 
						 file_voxel.x + slice_url_ext ),
					buff,
					volume_header.getSizeY(),
					consumer, 
					file_voxel.x );
		    }
		    catch( Exception e) {
			System.err.println( e);
			return;
		    }
		    _saveSlab( buff, 
			       std_dim_order, std_dim_perm, 
			       new int[] { 0, 0, file_voxel.x}, 
			       new int[] { volume_header.getSizeZ(), volume_header.getSizeY(), 1});
		    s_slice_downloaded[ file_voxel.x]= true;

		    if( consumer != null )
			consumer.sliceDataUpdated( x);		    
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
	boolean already_downloaded;
	Point3Dfloat world= common_sampling.voxel2world( 0, y, 0);
	final Point3Dint file_voxel= volume_header.world2voxel( world);

	if( file_voxel.y < 0 || file_voxel.y >= c_slice_downloaded.length)
	    already_downloaded= true;
	else
	    already_downloaded= c_slice_downloaded[ file_voxel.y];

	final int x_size= getXSize();
	final int z_size= getZSize();
	byte[][][] voxels= this.voxels;
	// NB: this vertically flips the image...
	for( int z= z_size - 1, offset= 0; z >= 0; --z, offset += x_size)
	    System.arraycopy( voxels[ z][ y], 0, slice, offset, x_size);

	if( consumer != null )
	    consumer.sliceDataUpdated( y);

	if( already_downloaded )
	    return;

	// download the slice in a parallel (bg) thread
	Thread t= new Thread() {
		public void run() 
		{ 
		    byte[] buff= 
			new byte[ volume_header.getSizeZ() * volume_header.getSizeX()];

		    try { 
			_downloadSlice( new URL( slice_url_base + "/c/" + 
						 file_voxel.y + slice_url_ext ),
					buff,
					volume_header.getSizeX(),
					consumer, 
					file_voxel.y );
		    }
		    catch( Exception e) {
			System.err.println( e);
			return;
		    }
		    _saveSlab( buff, 
			       std_dim_order, std_dim_perm, 
			       new int[] { 0, file_voxel.y, 0}, 
			       new int[] { volume_header.getSizeZ(), 1, volume_header.getSizeX()});
		    c_slice_downloaded[ file_voxel.y]= true;

		    if( consumer != null )
			consumer.sliceDataUpdated( y);
		}
	    };
	t.start();
    }

    /**
     * @param voxel_value a 0..255 (byte) value
     * @return the image value (real value) corresponding to voxel_value
     */
    public final float voxel2image( short voxel_value) {
	return volume_header.voxel2image( voxel_value);
    }

    /**
     * @param image_value (aka real value)
     * @return the 0..255 voxel value corresponding to image_value 
     */
    public final short image2voxel( float image_value) {
	return volume_header.image2voxel( image_value);
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


    final /*private*/ void _downloadAllVolume( URL source_url)
	throws IOException, SecurityException
    {
	int[] dim_order= volume_header.getDimOrder();
	int[] dim_perm= volume_header.getDimPermutation();
	int[] sizes= volume_header.getSizes();
	// size along 1st dimension of the file
	int size_0= sizes[ dim_order[ 0]]; 
	// size along 2nd dimension of the file
	int size_1= sizes[ dim_order[ 1]]; 
	// size along 3rd (last) dimension of the file
	int size_2= sizes[ dim_order[ 2]]; 

	byte[] buff= new byte[ size_1 * size_2];
	int[] slab_start= new int[] { 0, 0, 0};
	final int[] slab_size=  new int[] { 1, size_1, size_2 };

	InputStream input_stream= null;
	try {
	    input_stream= Util.openURL( source_url);

	    /* download it one slice at a time (the read buffer is the
	       size of 1 slice) */
	    
	    for( int d_0= 0; d_0 < size_0; d_0++) {
		_readSlice( input_stream, 
			    /* todo: this is wasteful (catch and rewrite exception...) */
			    source_url.toString(),
			    buff, size_2, size_1);
		slab_start[ 0]= d_0;
		_saveSlab( buff, 
			   dim_order, dim_perm, 
			   slab_start, slab_size);
		slice_downloaded[ dim_order[0] ][ d_0 ]= true;
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
	for( i= 0; i < size_1; ++i)
	    slice_downloaded[ dim_order[1] ][ i ]= true;
	for( i= 0; i < size_2; ++i)
	    slice_downloaded[ dim_order[2] ][ i ]= true;

	all_data_downloaded= true;
    }

    final /*private*/ void _downloadSlice( URL source_url, 
					   byte[] slice, 
					   final int slice_width,
					   SliceImageProducer consumer, 
					   int slice_number )
	throws IOException, SecurityException
    {
	InputStream input_stream= null;
	try {
	    input_stream= Util.openURL( source_url);
	    _readSlice( input_stream, source_url.toString(), 
			slice, slice_width, slice.length/slice_width);
	    System.out.println( source_url + " loading done!");
	}
	finally {
	    if( input_stream != null) {
		input_stream.close();
		input_stream= null;
	    }
	}
    }

    final /*private*/ void _readSlice( final InputStream input_stream,
				       final String input_name,
				       byte[] slice_buff, 
				       final int slice_width,
				       final int slice_height )
	throws IOException
    {
	int line, read_count, left, offset;
	for( line= 0, offset= 0; line < slice_height; line++, offset += slice_width) {
	    left= slice_width;
	    while( left > 0) {
		if( DELAY_DOWNLOAD ) Util.sleep( 5); 
		/* the tricky bit here is that this read() sometimes
		   only reads part of the data, so we need to call it
		   again and again until we get all our data (btw,
		   this is _not_ documented anywhere!) */
		read_count= input_stream.read( slice_buff, 
					       offset + slice_width - left, 
					       left);
		if( read_count <= 0)
		    throw new IOException( input_name
					   + " : premature end of data : "
					   + line + " : " + read_count);
		left -= read_count;
	    }
	}
    }


    /** Copies/resamples a "hyperslab" into the internal
        data-structure, using the existing <code>resample</code>
        tables.
	
	@param slab the "hyperslab" data
	@param dim_order dimension order of the slab (@see
	VolumeHeader#getDimOrder)
	@param dim_perm (@see VolumeHeader#getDimPermutation)
	@param start slab start voxels (in slab dim order)
	@param size slab length along each dim (in slab order)
    */
    final /*private*/ void _saveSlab( byte[] slab, 
				      int[] dim_order,
				      int[] dim_perm,
				      int[] start,
				      int[] size )
    {
	// for speed, use a stack variable instead of the instance field:
	byte[][][] 	voxels= this.voxels;
	int		in_0, in_1, in_2;
	int[] 		out= new int[ 3];
	int[][] 	map_start= this.resample.start;
	int[][] 	map_end= this.resample.end;

	if( false && DEBUG ) {
	    System.out.println( Util.arrayToString( dim_order));
	    System.out.println( Util.arrayToString( dim_perm));
	    System.out.println( Util.arrayToString( start));
	    System.out.println( Util.arrayToString( size));
	    System.out.println( Util.arrayToString( map_start[0]));
	    System.out.println( Util.arrayToString( map_end[0]));
	    System.out.println( Util.arrayToString( map_start[1]));
	    System.out.println( Util.arrayToString( map_end[1]));
	    System.out.println( Util.arrayToString( map_start[2]));
	    System.out.println( Util.arrayToString( map_end[2]));
	}

	for( in_0= start[0]; in_0 < start[0]+size[0]; in_0++) 
	    for( out[0]= map_start[ dim_order[0]][ in_0]; out[0] <= map_end[ dim_order[0]][ in_0]; out[0]++ ) {

		for( in_1= start[1]; in_1 < start[1]+size[1]; in_1++) 
		    for( out[1]= map_start[ dim_order[1]][ in_1]; out[1] <= map_end[ dim_order[1]][ in_1]; out[1]++ ) {

			for( in_2= start[2]; in_2 < start[2]+size[2]; in_2++) 
			    for( out[2]= map_start[ dim_order[2]][ in_2]; out[2] <= map_end[ dim_order[2]][ in_2]; out[2]++ ) {

				final int slab_index= 
				    (in_0-start[0])*size[1]*size[2] + (in_1-start[1])*size[2] + (in_2-start[2]);

				if( false && DEBUG ) {
				    System.out.println( "");
				    System.out.println( out[dim_perm[2]] +" "+ out[dim_perm[1]] +" "+ out[dim_perm[0]]);
				    System.out.println( in_0 +" "+ in_1 +" "+ in_2 );
				    System.out.println( slab_index);
				}

				voxels[ out[dim_perm[2]] ][ out[dim_perm[1]] ][ out[dim_perm[0]] ]= 
				    slab[ slab_index];
				/* OUCH! :-) */
			    }

		    }

	    }

	// TODO? chk if all transverse slices were downloaded,
	// and then set all c_slice & s_slice to true also ...
    }

    /**
     * @return value of last element of <code>array</code>
     */
    final /*private*/ int _last( final int[] array ) 
    {
	return array[ array.length - 1 ];
    }

}

