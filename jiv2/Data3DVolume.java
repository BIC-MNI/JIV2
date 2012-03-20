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
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: Data3DVolume.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class Data3DVolume {

    /** shows which files are loaded */
    /*private*/ static final boolean    VERBOSE= true;

    /** for development only: see methods called when you click on a viewport! */
    /*private*/ static final boolean    DEBUG_TRACE= false;

    /** for development only: see what's happening */
    /*private*/ static final boolean 	DEBUG= false;
    /*private*/ static final boolean 	DEBUG_A= false;

    /** for development only: artificially delay the downloads */
    /*private*/ static final boolean DELAY_DOWNLOAD= false;

    public static final int DOWNLOAD_UPFRONT=		1;
    public static final int DOWNLOAD_ON_DEMAND= 	2;
    public static final int DOWNLOAD_HYBRID= 		3;

    /** slice_dirname[i][j] is the subdirectory name for the dim(i) x
        dim(j) slices, where dim is the "dimensions order" list of the
        file (as specified in the header). Note that (b x a) slices
        are stored in the same file as (a x b) slices -- _saveSlab()
        handles the difference. */
    static /*private*/ String[][]		slice_dirname;
    static {
	slice_dirname= new String[3][3];
	// "sagittal": (in a transverse volume: z y x)
	slice_dirname[0][1]= slice_dirname[1][0]= "01";
	// "coronal":
	slice_dirname[0][2]= slice_dirname[2][0]= "02";
	// "transverse":
	slice_dirname[1][2]= slice_dirname[2][1]= "12";
    }

    /*private*/ byte[][][] 	common_voxels;     // (z,y,x)!

    /*private*/ VolumeHeader.ResampleTable	resample_table;

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

    /** the full-volume (background) download thread */
    /*private*/ Thread 			bg_dnld;

    /*private*/ String 		volume_url;	// eg http://www/foo/colin27.raw.gz
    /*private*/ URL 		url_context; // eg http://www/foo/CONFIG
    /*private*/ String 		slice_url_base; // eg http://www/foo/colin27
    /*private*/ String 		slice_url_ext;  // eg .raw.gz

    /*private*/ String 		nick_name;
    /*private*/ boolean		isNative;

    /** header associated with the data in volume_url */
    /*private*/ VolumeHeader	volume_header;
    /*private*/ VolumeHeader	common_sampling;


    public Data3DVolume( VolumeHeader common_sampling,
			 VolumeHeader volume_header)
    {
	this.common_sampling= common_sampling;
	this.volume_header= volume_header;
	common_voxels= new byte[ getZSize()][ getYSize()][ getXSize()];
    }

    /** Note: it's not _required_ to declare SecurityException (since
     it's a subclass of RuntimeException), but we do it for clarity --
     this error is likely to happen when working with url-s...
    */
    public Data3DVolume( boolean isNative,
			 VolumeHeader common_sampling, 
			 URL url_context,
			 String volume_file_name,
			 VolumeHeader volume_header, 
			 String nick_name,
			 int download_method ) 
	throws IOException, SecurityException
    {
	this( common_sampling, volume_header);
	this.url_context= url_context;
	final URL source_url= new URL(url_context, volume_file_name);

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
	this.isNative= false;

	if (VERBOSE) System.out.println("*** NICKNAME: "+this.nick_name+" ***");
	resample_table= volume_header.getResampleTable( common_sampling);

	/* initialize this volume's region to the dummy pattern
	   ("solid color", more exactly), but only within the extend
	   of this file -- leave black padding outside... */
	{
	    int[][] 	map_start= this.resample_table.start;
	    int[][] 	map_end= this.resample_table.end;
	    int start_x, end_x, start_y, end_y, start_z, end_z;
	    /* need to use min/max because volume_header can sometimes have
               steps<0 */
	    start_x= Math.min( _first( map_start[0]), _last( map_start[0]));
	    end_x=   Math.max( _first( map_end[0]),   _last( map_end[0]));
	    start_y= Math.min( _first( map_start[1]), _last( map_start[1]));
	    end_y=   Math.max( _first( map_end[1]),   _last( map_end[1]));
	    start_z= Math.min( _first( map_start[2]), _last( map_start[2]));
	    end_z=   Math.max( _first( map_end[2]),   _last( map_end[2]));

	    if( DEBUG) System.out.println( this + " extents : " + start_x +" "+ end_x +" "+ start_y +" "+ end_y +" "+ start_z +" "+ end_z);

	    // Initialize the space with grey (255*0.2)
	    int 	line_len= end_x - start_x + 1;
	    byte[] 	dummy_line1= new byte[ line_len];
	    byte 	dummy_val= (byte)(255*0.2);
	    for( int i= 0; i < line_len; ++i) 
		dummy_line1[ i]= dummy_val;
	    for( int z= start_z; z <= end_z; ++z)
		for( int y= start_y; y <= end_y ; ++y)
		    System.arraycopy( dummy_line1, 0, 
				      common_voxels[ z][ y], start_x,
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
	    bg_dnld= new Thread() {
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
	    bg_dnld.setPriority( Thread.MIN_PRIORITY ); 
	    //bg_dnld.setPriority( Thread.currentThread().getPriority() - 2 ); 
	    bg_dnld.start();
	    break;

	default:
	    throw new 
		IllegalArgumentException( this + " unknown download method: " +	download_method);
	}
    }

    public void setNative(boolean isNative) {
	if (VERBOSE && isNative)
		System.out.println("Setting "+nick_name+" to be native.");
	this.isNative= isNative;
    }

    /** Kills the background download threads. (note: currently, only
        the lengthy full-volume download thread is killed...) 
    */
    final public void stopDownloads() 
    { 
	if( bg_dnld != null && bg_dnld.isAlive() ) {
	    bg_dnld.stop();
	}
	bg_dnld= null;

	/* Also killing the slice download threads would be nice, but
           it's not trivial: you need to store a ref to each new
           thread (when created by getTransverseSlice & co). This list
           of refs will keep growing, and will also prevent dead
           threads to be garbage-collected... */
    }

    final public int getXSize() { return common_sampling.getSizeX(); }

    final public int getYSize() { return common_sampling.getSizeY(); }

    final public int getZSize() { return common_sampling.getSizeZ(); }

    final public float getXStep() { return volume_header.getStepX(); }

    final public float getYStep() { return volume_header.getStepY(); }

    final public float getZStep() { return volume_header.getStepZ(); }

    final public String getNickName() { return nick_name; }

    /** This method should only be called when 'this' is the label volume.. */
    final public String getLabel(Point3Dfloat world_mni) {
	Point3Dint voxel= common_sampling.world2voxel(world_mni);
	int intensity= getVoxelAsInt(voxel.x, voxel.y, voxel.z);
	return CoordConv.intensity2label(intensity);
    }

    final public byte getVoxel( int x, int y, int z)
    {
	return common_voxels[ z][ y][ x];
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
    final public byte[] getTransverseSlice( final Point3Dfloat world, final int slice_z )
    {
	byte[] slice= new byte[ getYSize() * getXSize()];
	getTransverseSlice( world, slice_z, slice, null);
	return slice;
    }

    /** this overloaded version is easier on the heap & garbage collector because
	it encourages reuse of an already allocated 'slice' array (which has
	to be large enough to hold the result, naturally).

	<em>Note:</em> param 'slice' should not be used outside the 
	current thread!!
    */
    final public void getTransverseSlice( final Point3Dfloat world,
					  final int common_z,
					  byte[] slice,
					  final SliceImageProducer consumer )
    {

	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^getTransverseSlice");

	boolean already_downloaded;

	/* TODO: you can use if( all_data_downloaded ) to speed this
           up for the common situation... */

	/* grossly inefficient... (TODO: use pre-allocated
	   fields... -- this won't be thread-safe, but is it an
	   issue??? do I ever have multiple threads calling this
	   method??) -- Yes (callback to sliceDataUpdated from the
	   slice download threads) */

	if (DEBUG_A) System.out.println("5 world2voxel to get local (not common) voxel coords in Data3DVolume for new slice");
	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^^getTransverseSlice -> volume_header.world2voxel()");
	final Point3Dint file_voxel= volume_header.world2voxel( world);

	if( file_voxel.z < 0 || file_voxel.z >= t_slice_downloaded.length)
	    already_downloaded= true;
	else
	    already_downloaded= t_slice_downloaded[ file_voxel.z];

	final int x_size= getXSize();
	final int y_size= getYSize();
	byte[][][] common_voxels= this.common_voxels;
	// NB: this vertically flips the image...
	// (decreasing index loops are rumored to run faster in Java)

	for( int y= y_size - 1, offset= 0; y >= 0; --y, offset += x_size)
	    System.arraycopy( common_voxels[ common_z][ y], 0, slice, offset, x_size);

	
	// If not already stored locally, download from source..
	if( already_downloaded ) {
	    if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^getTransverseSlice DONE!\n");
	    return;
	}

	// download the slice in a parallel (bg) thread
	Thread t= new Thread() {
		public void run() {
		    if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^^getTransverseSlice -> _asyncDownloadSlice");
		    _asyncDownloadSlice( 1, 0, file_voxel.z, consumer, common_z); 
		}
	    };
	// TODO: maybe use (MIN_PRIORITY+1), or even MIN_PRIORITY, instead???
	t.setPriority( Thread.currentThread().getPriority() - 1 ); 
	t.start();

	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^getTransverseSlice DONE!\n");
    }

    final public byte[] getSagittalSlice( final Point3Dfloat world, final int x)
    {
	byte[] slice= new byte[ getZSize() * getYSize()];
	getSagittalSlice( world, x, slice, null);
	return slice;
    }

    final public void getSagittalSlice( final Point3Dfloat world,
					final int common_x,
					byte[] slice,
					final SliceImageProducer consumer)
    {

	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^getSagittalSlice");

	boolean already_downloaded;

	if (DEBUG_A) System.out.println("5 world2voxel to get local (not common) voxel coords in Data3DVolume for new slice");
	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^^getSagittalSlice -> volume_header.world2voxel()");
	final Point3Dint file_voxel= volume_header.world2voxel( world);

	if( file_voxel.x < 0 || file_voxel.x >= s_slice_downloaded.length)
	    already_downloaded= true;
	else
	    already_downloaded= s_slice_downloaded[ file_voxel.x];

	final int y_size= getYSize();
	final int z_size= getZSize();
	byte[][][] common_voxels= this.common_voxels;

	// NB: this vertically flips the image...
	for( int z= z_size - 1, offset= 0; z >= 0; --z, offset += y_size) 
	    for( int y= y_size - 1; y >= 0; --y)
		slice[ offset + y]= common_voxels[ z][ y][ common_x];

	if( already_downloaded ) {
	    if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^getSagittalSlice DONE!\n");
	    return;
	}

	// download the slice in a parallel (bg) thread
	Thread t= new Thread() {
		public void run() {
		    if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^^getSagittalSlice -> _asyncDownloadSlice");
		    _asyncDownloadSlice( 2, 1, file_voxel.x, consumer, common_x); 
		}
	    };
	t.setPriority( Thread.currentThread().getPriority() - 1 ); 
	t.start();

	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^getSagittalSlice DONE!\n");
    }

    final public byte[] getCoronalSlice( final Point3Dfloat world, final int y )
    {
	byte[] slice= new byte[ getZSize() * getXSize()];
	getCoronalSlice( world, y, slice, null);
	return slice;
    }

    final public void getCoronalSlice( final Point3Dfloat world,
				       final int common_y,
				       byte[] slice,
				       final SliceImageProducer consumer)
    {

	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^getCoronalSlice");

	boolean already_downloaded;

	if (DEBUG_A) System.out.println("Data3DVolume.getCoronalSlice -> volume_header.world2voxel(world)");
	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^^getCoronalSlice -> volume_header.world2voxel()");
	final Point3Dint file_voxel= volume_header.world2voxel( world);

	if( file_voxel.y < 0 || file_voxel.y >= c_slice_downloaded.length)
	    already_downloaded= true;
	else
	    already_downloaded= c_slice_downloaded[ file_voxel.y];

	final int x_size= getXSize();
	final int z_size= getZSize();
	byte[][][] common_voxels= this.common_voxels;

	// NB: this vertically flips the image...
	for( int z= z_size - 1, offset= 0; z >= 0; --z, offset += x_size)
	    System.arraycopy( common_voxels[ z][ common_y], 0, slice, offset, x_size);

	if( already_downloaded ) {
	    if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^getCoronalSlice DONE!\n");
	    return;
	}
	// download the slice in a parallel (bg) thread
	Thread t= new Thread() {
		public void run() {
		    if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^^getCoronalSlice -> _asyncDownloadSlice()");
		    _asyncDownloadSlice( 2, 0, file_voxel.y, consumer, common_y); 
		}
	    };
	t.setPriority( Thread.currentThread().getPriority() - 1 ); 
	t.start();

	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t\t^getCoronalSlice DONE!\n");
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
	int min= common_voxels[ 0][ 0][ 0];
	int max= common_voxels[ 0][ 0][ 0];
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
			    /* todo: this is kind of wasteful (maybe
                               catch and rewrite exception...) : */
			    source_url.toString(),
			    buff, size_2, size_1);

		slab_start[ 0]= d_0;

		if( this.resample_table.fast_resample )
		    _saveSlab_fast( buff, slab_start, slab_size);
		else
		    _saveSlab( buff, slab_start, slab_size);

		slice_downloaded[ dim_order[0] ][ d_0 ]= true;
		/* this should help on a slow & non-preemptive jvm ... */
		Thread.yield();
	    }
	    if (VERBOSE) System.out.println( source_url + " loading done!");
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

    /**
     * @param vert_dim canonical vertical dimension of the slice (0 for x, etc)
     * @param horiz_dim canonical horizontal dimension of the slice
     * @param input_slice_no voxel coordinate of this slice in the input volume file
     * @param consumer_slice_no voxel coordinate of this slice in the common sampling coordinates???
     */
    final /*private*/ void _asyncDownloadSlice( int vert_dim, 
						int horiz_dim, 
						int input_slice_no, 
						SliceImageProducer consumer, 
						int consumer_slice_no ) 
    {
	if (DEBUG) {
		System.out.println("_asyncDownloadSlice..");
		System.out.println("\tinput_slice_no (jpg): "+input_slice_no);
		System.out.println("\tconsumer_slice_no: "+consumer_slice_no);
		System.out.println("\tconsumer: "+consumer);
	}
	int[] dim_perm= volume_header.getDimPermutation();
	int[] sizes= volume_header.getSizes();
	int size_v= sizes[ vert_dim];
	int size_h= sizes[ horiz_dim];
	String dir= slice_dirname[ dim_perm[ vert_dim]][ dim_perm[ horiz_dim]];
	
	byte[] buff= new byte[ size_v * size_h];
	try { 
	    _downloadSlice( new URL( slice_url_base + "/" + dir + "/" +
				     input_slice_no + slice_url_ext ),
			    buff, size_v, size_h );
	}
	catch( Exception e) {
	    System.err.println( e);
	    return;
	}

	// the other canonical dimension (orthogonal to the slice)
	int ortho_dim; 
	/* the following assumes that vert_dim and horiz_dim are
           different and in 0..2 */
	for( ortho_dim= 0; ortho_dim < 3; ++ortho_dim) 
	    if( ortho_dim != vert_dim && ortho_dim != horiz_dim )
		// found it!
		break;

	// the rest should be synchronized( Data3DVolume.this)
	// but nothing bad will happen if two parallel updates occur
	// (the info being written is identical)
	// TODO: verify this!!!

	int[] slab_start= new int[] { 0, 0, 0}; 
	slab_start[ dim_perm[ ortho_dim]]= input_slice_no;
	int[] slab_size= new int[ 3];
	slab_size[ dim_perm[ vert_dim]]= size_v;
	slab_size[ dim_perm[ horiz_dim]]= size_h;
	slab_size[ dim_perm[ ortho_dim]]= 1;

	//if( vert_dim == 1 && horiz_dim == 0)
	_saveSlab( buff, slab_start, slab_size);

	slice_downloaded[ ortho_dim][ input_slice_no]= true;

	if( consumer != null )
	    consumer.sliceDataUpdated( consumer_slice_no);
    }

    final /*private*/ void _downloadSlice( URL source_url, 
					   byte[] slice, 
					   final int slice_width,
					   final int slice_height )
	throws IOException, SecurityException
    {
	InputStream input_stream= null;
	try {
	    input_stream= Util.openURL( source_url);
	    _readSlice( input_stream, source_url.toString(), 
			slice, slice_width, slice_height);
	    if( DEBUG) System.out.println( source_url + " loading done!");
	}
	finally {
	    if( input_stream != null) {
		input_stream.close();
		input_stream= null;
	    }
	}
    }

    /** Note: since the slice buffer and the file are both
     * 1-dimensional, it doesn't matter if the image file is in fact
     * (width x height) or (height x width) 
     */
    final /*private*/ void _readSlice( final InputStream input_stream,
				       final String input_name,
				       byte[] slice_buff, 
				       final int slice_width,
				       final int slice_height )
	throws IOException
    {
	int total= slice_width * slice_height;
	int left= total;
	int read_count;
	while( left > 0) {
	    if( DELAY_DOWNLOAD ) Util.sleep( 800); 
	    /* the tricky bit here is that this read() sometimes
	       only reads part of the data, so we need to call it
	       again and again until we get all our data (btw,
	       this is _not_ documented anywhere!) */
	    read_count= input_stream.read( slice_buff, 
					   total - left, 
					   left);
	    if( read_count <= 0)
		throw new IOException( input_name
				       + " : premature end of data : "
				       + left + " : " + read_count);
	    left -= read_count;
	}
    }


    /** Copies/resamples a "hyperslab" into the internal
        data-structure, using the existing <code>resample</code>
        tables.
	
	@param slab the "hyperslab" data
	@param start slab start voxels (in slab dim order)
	@param size slab length along each dim (in slab order)
    */
    final /*private*/ void _saveSlab( byte[] slab, 
				      int[] start,
				      int[] size )
    {
	int[] 		dim_order= volume_header.getDimOrder();
	int[] 		dim_perm= volume_header.getDimPermutation();
	// for speed, use a stack variable instead of the instance field:
	byte[][][] 	common_voxels= this.common_voxels;
	int		in_0, in_1, in_2;
	int[] 		out= new int[ 3];
	int[][] 	map_start= this.resample_table.start;
	int[][] 	map_end= this.resample_table.end;

	if( false && DEBUG ) {
	    System.out.println("_saveSlab");
	    System.out.println("start: "+Util.arrayToString( start));
	    System.out.println("size: "+Util.arrayToString( size));
	    System.out.println("map_start x: "+Util.arrayToString( map_start[0]));
	    System.out.println("map_end x: "+Util.arrayToString( map_end[0]));
	    System.out.println("map_start y: "+Util.arrayToString( map_start[1]));
	    System.out.println("map_end y: "+Util.arrayToString( map_end[1]));
	    System.out.println("map_start z: "+Util.arrayToString( map_start[2]));
	    System.out.println("map_end z: "+Util.arrayToString( map_end[2]));
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

				common_voxels[ out[dim_perm[2]] ][ out[dim_perm[1]] ][ out[dim_perm[0]] ]= 
				    slab[ slab_index];
				/* OUCH! :-) */
			    }

		    }

	    }

	// TODO? chk if all transverse slices were downloaded,
	// and then set all c_slice & s_slice to true also ...
    }

    /** Version of _saveSlab for when
	(this.resample_table.fast_resample == true).  NB: It assumes
	the slab is in y-x order, and 1 voxel thick (size[0]==1) !!

	It's basically a (ugly) speed hack for lousy slow JVM-s.

	@param slab the "hyperslab" data
	@param start slab start voxels (in slab dim order)
	@param size slab length along each dim (in slab order) 
    */
    final /*private*/ void _saveSlab_fast( byte[] slab, 
					   int[] start,
					   int[] size )
    {
	// for speed, use a stack variable instead of the instance field:
	byte[][][] 	common_voxels= this.common_voxels;
	int[][] 	map_start= this.resample_table.start;
	int[][] 	map_end= this.resample_table.end;

	final int x_start= map_start[0][ start[2]];
	final int y_start= map_start[1][ start[1]];
	final int z_start= map_start[2][ start[0]];
	final int x_size= size[2];
	final int y_size= size[1];
	final int y_end= y_start + y_size;

	for( int y= y_start, offset= 0; y < y_end; ++y, offset += x_size)
	    System.arraycopy( slab, offset, common_voxels[ z_start][ y], x_start, x_size);
    }

    
    /**
     * @return value of first element of <code>array</code>
     */
    final static /*private*/ int _first( final int[] array ) 
    {
	return array[ 0];
    }

    /**
     * @return value of last element of <code>array</code>
     */
    final static /*private*/ int _last( final int[] array ) 
    {
	return array[ array.length - 1 ];
    }

    public String toString(){
	return nick_name;
    }

}

