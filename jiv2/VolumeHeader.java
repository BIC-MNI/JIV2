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
import java.util.*;

/**
 * Loads, stores, and provides access to the header info of a 
 * 3D image volume.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: VolumeHeader.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class VolumeHeader {

    /*private*/ static final boolean 	DEBUG= false;
    /*private*/ static final boolean 	DEBUG_INVERSE= false;
    /*private*/ static final boolean 	DEBUG_WV= false;

static final boolean RETURN_0=true;
static final boolean RETURN_MID=false;
static final boolean RETURN_MID_OFFSET=false;

    /*private*/ static final boolean 	USE_MAX= false;

    /*private*/ float start_x;
    /*private*/ float start_y;
    /*private*/ float start_z;

    /*private*/ float step_x;
    /*private*/ float step_y;
    /*private*/ float step_z;

    /*private*/ int size_x;
    /*private*/ int size_y;
    /*private*/ int size_z;

    /** @see #getDimOrder */
    /*private*/ int[] dim_order;

    /*private*/ float image_low;
    /*private*/ float image_high;

    /*private*/ float[] dir_cosines;

    /*private*/ float[][] VOX2WORLD;
    /*private*/ float[][] WORLD2VOX;


    /** The default is the standard "MNI-ICBM-Talairach" (181x217x181)
	   sampling. */
    public VolumeHeader() {
	
	start_x=  -90f;
	start_y= -126f;
	start_z=  -72f; 
	step_x=     1f;
	step_y=     1f;
	step_z=     1f;
	size_x=   181;
	size_y=   217;
	size_z=   181;
	dim_order= new int[] { 2, 1, 0 }; // "transverse" volume ordering

	image_low=  0.0f;
	image_high= 1.0f;

	dir_cosines= new float[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
	VOX2WORLD= new float[][] { {1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0} };
	WORLD2VOX= new float[][] { {1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0} };
    }

    /** copy constuctor */
    public VolumeHeader( VolumeHeader src ) {
	
	start_x=  src.start_x;
	start_y=  src.start_y;
	start_z=  src.start_z;
	step_x=   src.step_x;
	step_y=   src.step_y;
	step_z=   src.step_z;
	size_x=   src.size_x;
	size_y=   src.size_y;
	size_z=   src.size_z;
	System.arraycopy( src.dim_order, 0, (dim_order= new int[3]), 0, 3);
	image_low=  src.image_low;
	image_high= src.image_high;
	System.arraycopy( src.dir_cosines, 0, (dir_cosines= new float[9]), 0, 9);
	System.arraycopy( src.VOX2WORLD, 0, (VOX2WORLD= new float[3][4]), 0, 3);
	System.arraycopy( src.WORLD2VOX, 0, (WORLD2VOX= new float[3][4]), 0, 3);
    }

    public VolumeHeader( URL source_url) 
	throws IOException, NumberFormatException, SecurityException {

	this();
	if( source_url == null ) 
	    // stick with the defaults...
	    return;

	//Initialize dir_cosines in case its not specified in the header
	dir_cosines= new float[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 };

	Properties header= Util.readProperties( source_url, null);

	/* todo: the following is ugly and should be rewritten some
           day using a parser and a parse table (ie: "programming with
           data", aka "capture regularity in the code, and
           irregularity in the data") ! */

	for( Enumeration keys= header.propertyNames(); keys.hasMoreElements(); ) {
	    String key= (String)keys.nextElement();
	    StringTokenizer values= 
		new StringTokenizer( header.getProperty( key), " ,\t", false);
	    String s1, s2, s3;

	    if( key.equals( "size")) {
		// these can throw NoSuchElementException
		s1= values.nextToken();
		s2= values.nextToken();
		s3= values.nextToken();
		// these can throw NumberFormatException
		size_x= Integer.valueOf( s1).intValue();
		size_y= Integer.valueOf( s2).intValue();
		size_z= Integer.valueOf( s3).intValue();
		if( size_x <= 0 || size_y <= 0 || size_z <= 0 )
		    throw new NumberFormatException( "size : ");
	    }
	    else if( key.equals( "start")) {
		s1= values.nextToken();
		s2= values.nextToken();
		s3= values.nextToken();
		start_x= Float.valueOf( s1).floatValue();
		start_y= Float.valueOf( s2).floatValue();
		start_z= Float.valueOf( s3).floatValue();
	    }
	    else if( key.equals( "step")) {
		s1= values.nextToken();
		s2= values.nextToken();
		s3= values.nextToken();
		step_x= Float.valueOf( s1).floatValue();
		step_y= Float.valueOf( s2).floatValue();
		step_z= Float.valueOf( s3).floatValue();
	    }
	    else if( key.equals( "order")) {
		Hashtable h= new Hashtable();
		for( int i= 0; i < 3; ++i) {
		    s1= values.nextToken();
		    switch( s1.charAt( 0)) {
		    case 'x':
		    case 'X':
			dim_order[ i]= 0; break;
		    case 'y':
		    case 'Y':
			dim_order[ i]= 1; break;
		    case 'z':
		    case 'Z':
			dim_order[ i]= 2; break;
		    default:
			throw new IOException( "order : expected one of x,y,z");
		    }
		    Object o= (Object)String.valueOf( dim_order[ i]);
		    h.put( o, o);
		}
		if( h.size() != 3 )
		    throw new IOException( "order : duplicate of one of x,y,z");
	    }
	    else if( key.equals( "imagerange")) {
		s1= values.nextToken();
		s2= values.nextToken();
		image_low= Float.valueOf( s1).floatValue();
		image_high= Float.valueOf( s2).floatValue();

		if( image_low > image_high) 
		    throw new IOException( "invalid imagerange: a > b");

		if( Float.isInfinite( image_low) || 
		    Float.isInfinite( image_high)   ) 
		    throw new IOException( "invalid imagerange: infinite value (for the 32bit IEEE 754 floating point format)");
	    }
	    else if( key.equals( "xspace_direction_cosines")) {
		s1= values.nextToken();
		s2= values.nextToken();
		s3= values.nextToken();
		if (DEBUG) System.out.println("xspace_direction_cosines: "+s1+" "+s2+" "+s3);
		dir_cosines[ 0]= Float.valueOf( s1).floatValue();
		dir_cosines[ 1]= Float.valueOf( s2).floatValue();
		dir_cosines[ 2]= Float.valueOf( s3).floatValue();
	    }
	    else if( key.equals( "yspace_direction_cosines")) {
		s1= values.nextToken();
		s2= values.nextToken();
		s3= values.nextToken();
		if (DEBUG) System.out.println("yspace_direction_cosines: "+s1+" "+s2+" "+s3);
		dir_cosines[ 3]= Float.valueOf( s1).floatValue();
		dir_cosines[ 4]= Float.valueOf( s2).floatValue();
		dir_cosines[ 5]= Float.valueOf( s3).floatValue();
	    }
	    else if( key.equals( "zspace_direction_cosines")) {
		s1= values.nextToken();
		s2= values.nextToken();
		s3= values.nextToken();
		if (DEBUG) System.out.println("zspace_direction_cosines: "+s1+" "+s2+" "+s3);
		dir_cosines[ 6]= Float.valueOf( s1).floatValue();
		dir_cosines[ 7]= Float.valueOf( s2).floatValue();
		dir_cosines[ 8]= Float.valueOf( s3).floatValue();
	    }
	    else
		throw new IOException( "invalid key: " + key);
	}

	if( DEBUG) System.out.println( toString());

	VOX2WORLD= computeVoxeltoWorld();
	WORLD2VOX= Util.invertMatrix(VOX2WORLD);

    }

    private final float[][] computeVoxeltoWorld(){
	float[][] result= new float[3][4];
	result[0][0]= dir_cosines[0]*step_x;
	result[0][1]= dir_cosines[3]*step_y;
	result[0][2]= dir_cosines[6]*step_z;
	result[0][3]= dir_cosines[0]*start_x + dir_cosines[3]*start_y + dir_cosines[6]*start_z;
	result[1][0]= dir_cosines[1]*step_x;
	result[1][1]= dir_cosines[4]*step_y;
	result[1][2]= dir_cosines[7]*step_z;
	result[1][3]= dir_cosines[1]*start_x + dir_cosines[4]*start_y + dir_cosines[7]*start_z;
	result[2][0]= dir_cosines[2]*step_x;
	result[2][1]= dir_cosines[5]*step_y;
	result[2][2]= dir_cosines[8]*step_z;
	result[2][3]= dir_cosines[2]*start_x + dir_cosines[5]*start_y + dir_cosines[8]*start_z;

	return result;
    }




    /** canonical (true) X ... */
    public final float getStartX() { return start_x; }
    public final float getStartY() { return start_y; }
    public final float getStartZ() { return start_z; }
    /** result in (canonical) x,y,z order ... */
    public final float[] getStarts() { return new float[] { start_x, start_y, start_z}; }

    public final float getStepX() { return step_x; }
    public final float getStepY() { return step_y; }
    public final float getStepZ() { return step_z; }
    /** result in (canonical) x,y,z order ... */
    public final float[] getSteps() { return new float[] { step_x, step_y, step_z}; }

    public final int getSizeX() { return size_x; }
    public final int getSizeY() { return size_y; }
    public final int getSizeZ() { return size_z; }
    /** result in (canonical) x,y,z order ... */
    public final int[] getSizes() { return new int[] { size_x, size_y, size_z}; }


    /* DimOrder and DimPermutation are one-to-one mappings between
       {0,1,2}; one is the inverse mapping of the other */


    /** DimOrder[i] = which canonical dimension is i-th dimension of
        the file. Canonical dim. 0 is 'x', 1 is 'y', 2 is 'z'. 

	Eg: {1,2,0} means 'y,z,x' ordering (x changes fastest) 

	Use this mapping to convert a list from canonical order to
	file order.  
    */
    public final int[] getDimOrder() { 

	int[] result= new int[3];
	System.arraycopy( dim_order, 0, result, 0, 3);
	return result;
    }

    /** DimPermutation[i] = the position of canonical dimension i in
	DimOrder (ie in the list of dimensions in file
	order). Canonical dim. 0 is 'x', 1 is 'y', 2 is 'z'.

	Eg: {2,0,1} means 'y,z,x' ordering (x changes fastest) 

	Use this mapping to convert a list from file order to
	canonical order.  
    */
    public final int[] getDimPermutation() { 
	
	int[] result= new int[3];

        next_dim:
	for( int dim= 0; dim < 3; ++dim) 
	    for( int pos= 0; pos < 3; ++pos) 
		if( dim_order[ pos] == dim ) {
		    result[ dim]= pos;
		    continue next_dim;
		}
	return result;
    }

    public String toString() {

	MultiLineStringBuffer buf= new MultiLineStringBuffer();
	buf.append_line( "VolumeHeader:");
	buf.append_line( "\t start: " + start_x +" "+ start_y +" "+ start_z); 
	buf.append_line( "\t step: " + step_x +" "+ step_y +" "+ step_z); 
	buf.append_line( "\t size: " + size_x +" "+ size_y +" "+ size_z); 
	buf.append_line( "\t dim_order: " + Util.arrayToString( dim_order));
	buf.append_line( "\t imagerange: " + image_low +" "+ image_high);
	buf.append_line( "\t dir_cosines: " + Util.arrayToString( dir_cosines));
	buf.append_line( "\t VOX2WORLD: " + Util.arrayToString( VOX2WORLD));
	buf.append_line( "\t WORLD2VOX: " + Util.arrayToString( WORLD2VOX));

	return buf.toString();
    }

    /**
     * @param samplings list of <code>VolumeHeader</code>-s
     * @return the common sampling : has an isotropic (and positive)
     * step equal to the smallest of them, and start/count-s that
     * cover the extent of all volumes 
     */
    static public final VolumeHeader getCommonSampling( Enumeration samplings) {

//### This is where the common step, start_x and x_size (according to global FOV) are set..
//### Need to do this separately for mni and native? Or would this screw EVERYTHING up?

	VolumeHeader ret= new VolumeHeader();

	Vector v= new Vector();
	while( samplings.hasMoreElements() ) 
	    v.addElement( samplings.nextElement());
	if( v.isEmpty())
	    throw new IllegalArgumentException( "empty argument list");

	/* the common step is the smallest step across all dimensions
           and all volumes */
	float common_step= Float.POSITIVE_INFINITY;
	for( Enumeration e= v.elements(); e.hasMoreElements(); ) {
	    VolumeHeader vh= (VolumeHeader)e.nextElement();
	    float[] steps= vh.getSteps();
	    for( int i= 0; i < 3; ++i) 
		common_step= Math.min( common_step, Math.abs( steps[i]));
	}
	ret.step_x= ret.step_y= ret.step_z= common_step;

	if( DEBUG) 
	    System.out.println( "getCommonSampling:\n\t common_step: " + common_step);

	for( int dim= 0; dim < 3; ++dim) {
	    float min= Float.POSITIVE_INFINITY;
	    float max= Float.NEGATIVE_INFINITY;

	    for( Enumeration e= v.elements(); e.hasMoreElements(); ) {
		VolumeHeader vh= (VolumeHeader)e.nextElement();
		float[] starts= vh.getStarts();
		float[] steps= vh.getSteps();
		int[] sizes= vh.getSizes();

		// NB: steps can be negative...
		float[] xtremes= new float[] { 
		    starts[ dim] - steps[ dim]/2.0f, 
		    starts[ dim] + steps[ dim]*(sizes[ dim] - 1) + steps[ dim]/2.0f
		};

		for( int x= 0; x < 2; ++x) {
		    min= Math.min( min, xtremes[ x]);
		    max= Math.max( max, xtremes[ x]);
		}
	    }
	    /* Now min&max indicate the extend of the field-of-view
               encompasing all volumes. The centers of the first&last
               voxels are common_step/2 mm "inwards" */
	    min += common_step/2.0f;
	    max -= common_step/2.0f;

	    if( DEBUG) 
		System.out.println( "\t min max: " + min +" "+ max);

	    switch( dim ) {
	    case 0 :
		ret.start_x= min;
		ret.size_x= 1 + (int)Math.ceil( (max - min)/ret.step_x);
		break;
	    case 1 :
		ret.start_y= min;
		ret.size_y= 1 + (int)Math.ceil( (max - min)/ret.step_y);
		break;
	    case 2 :
		ret.start_z= min;
		ret.size_z= 1 + (int)Math.ceil( (max - min)/ret.step_z);
		break;
	    }
	}

	ret.dim_order= new int[] { 2, 1, 0 }; // "transverse" volume ordering

	// these are volume-specific ...
	ret.image_low= ret.image_high= Float.NaN;

	ret.VOX2WORLD= ret.computeVoxeltoWorld();
        ret.WORLD2VOX= Util.invertMatrix(ret.VOX2WORLD);


	if( DEBUG) System.out.println( ret.toString());

	return ret;
    }

    /**
     * @return world coordinates of the center of the field-of-view
     * covered by this volume.
     */
    public final Point3Dfloat getFOVCenter() {

	return new Point3Dfloat( start_x + size_x/2 * step_x,
				 start_y + size_y/2 * step_y,
				 start_z + size_z/2 * step_z );
    }

    /** @see #getResampleTable */
    public final class ResampleTable {

	/** first index is the canonical dimension (0 for x, etc) */
	public int[][] 	start;
	public int[][] 	end;

	/** set if all volume steps are same as common step (implies
            isotropic voxels, and positive steps/dir), and dimension
            ordering is 'z y x' */
	public boolean	fast_resample= false;

	public ResampleTable( int[] sizes) {

	    start= new int[ sizes.length][];
	    end= new int[ sizes.length][];

	    for( int i= 0; i < sizes.length; ++i) {
		start[ i]= new int[ sizes[ i]];
		end[ i]= new int[ sizes[ i]];
	    }
	}
    }

    /**
     * @return a <code>ResampleTable</code> that can be used for
     * resampling the input file data having <code>this</code>
     * sampling onto the <code>common_sampling</code> grid: a file
     * voxel with x=i should be copied into the following voxels of
     * the common_sampling representation: start_x[i]...end_x[i]
     * (where start_x=start[0], etc)
     *
     * ASSUMPTION: <code>common_sampling</code> should be the same, or
     * finer sampling than the file's -- ie the common step is never
     * larger than file's step.  
     */
    public final ResampleTable getResampleTable( VolumeHeader common_sampling) {

	int[] cs_sizes= common_sampling.getSizes();
	int[] local_sizes= this.getSizes();

	if (DEBUG) System.out.println("cs_sizes: "+Util.arrayToString(cs_sizes));
	if (DEBUG) System.out.println("local_sizes: "+Util.arrayToString(local_sizes));

	int cs_max_dim0= cs_sizes[0];
	int cs_max_dim1= cs_sizes[1];
	int cs_max_dim2= cs_sizes[2];

	ResampleTable rt= new ResampleTable( local_sizes);
	int local_vox, last_vox;

	for( int dim= 0; dim < 3; ++dim) {
	    local_vox= last_vox= -1;
	    for( int cs_vox= 0; cs_vox < cs_sizes[ dim]; ++cs_vox) {

		switch( dim) {
		case 0 :
		    local_vox= getThisXFromCommonX( cs_vox, cs_max_dim1, cs_max_dim2, common_sampling);
		    break;
		case 1 :
		    local_vox= getThisYFromCommonY( cs_vox, cs_max_dim0, cs_max_dim2, common_sampling);
		    break;
		case 2 :
		    local_vox= getThisZFromCommonZ( cs_vox, cs_max_dim0, cs_max_dim1, common_sampling);
		    break;
		}
		if( local_vox < 0 || local_vox >= local_sizes[ dim] )
		    continue;

		//Whenever cs_vox maps to a higher local_vox, set rt.start and rt.end ...
		// when cs_vox is increased but still maps to the same local_vox, just set rt.end
		if( local_vox != last_vox) {
		    // new voxel

		    rt.start[ dim][ local_vox]= cs_vox;
		    rt.end[ dim][ local_vox]= cs_vox;
		    last_vox= local_vox;
		}
		else {
		    // start stays the same, end gets updated
		    rt.end[ dim][ last_vox]= cs_vox;
		}
	    }// end for cs_vox++

	}// end for dim++

	float css= common_sampling.step_x;
	int[] d_o= this.getDimOrder();

	//To allow a fast resample, each of the step sizes must be the same as the isotropic common step size
	//and, the dimension order must be z y x (fastest changing is x)
	rt.fast_resample= 
	    // fixme: comparing 2 float with == is asking for trouble...
	    Util.floatEquals(this.getStepX(), css) && Util.floatEquals(this.getStepY(), css) && Util.floatEquals(this.getStepZ(), css)
	    &&
	    // 'z y x' dimension ordering:
	    d_o[ 0] == 2 && d_o[ 1] == 1 && d_o[ 2] == 0 ;
	    
	return rt;
    }

    private final int getThisXFromCommonX( int cs_vox, int cs_max_dim1, int cs_max_dim2, VolumeHeader common_sampling) {
//System.out.println("converting common x ("+cs_vox+") into local x..");
	Point3Dfloat world1= new Point3Dfloat();
	Point3Dfloat world2= new Point3Dfloat();
	Point3Dfloat world3= new Point3Dfloat();
	Point3Dfloat world4= new Point3Dfloat();
	Point3Dint voxel1= new Point3Dint();
	Point3Dint voxel2= new Point3Dint();
	Point3Dint voxel3= new Point3Dint();
	Point3Dint voxel4= new Point3Dint();
	common_sampling.voxel2world( world1, cs_vox, cs_max_dim1, cs_max_dim2);
	common_sampling.voxel2world( world2, cs_vox, cs_max_dim1, 0);
	common_sampling.voxel2world( world3, cs_vox, 0, cs_max_dim2);
	common_sampling.voxel2world( world4, cs_vox, 0, 0);
//System.out.println("\tworld1: "+world1+" world2: "+world2);
//System.out.println("\tworld3: "+world3+" world4: "+world4);
	this.world2voxel( voxel1, world1);
	this.world2voxel( voxel2, world2);
	this.world2voxel( voxel3, world3);
	this.world2voxel( voxel4, world4);
//System.out.println("\tvoxel1.x: "+voxel1.x+" voxel2.x: "+voxel2.x+" voxel3.x: "+voxel3.x+" voxel4.x: "+voxel4.x);
	int min_x= Math.max( voxel1.x, voxel2.x);
	min_x= Math.min( min_x, voxel3.x);
	min_x= Math.min( min_x, voxel4.x);
	int max_x= Math.max( voxel1.x, voxel2.x);
	max_x= Math.max( max_x, voxel3.x);
	max_x= Math.max( max_x, voxel4.x);
//System.out.println("\tmin_x: "+min_x+" max_x:"+max_x+" return: "+((max_x-min_x)/2 + min_x));
//	common_sampling.voxel2world( world1, cs_vox, 166, 108);
//	this.world2voxel( voxel1, world1);
if (RETURN_0)
	return voxel4.x;
else if (RETURN_MID)
	return ((max_x-min_x)/2 + min_x);
else if (RETURN_MID_OFFSET)
	return ((max_x-min_x)/2 + min_x - 2);
else
	return voxel4.x;
    }

    private final int getThisYFromCommonY( int cs_vox, int cs_max_dim0, int cs_max_dim2, VolumeHeader common_sampling) {
//System.out.println("converting common y ("+cs_vox+") into local y..");
	Point3Dfloat world1= new Point3Dfloat();
	Point3Dfloat world2= new Point3Dfloat();
	Point3Dfloat world3= new Point3Dfloat();
	Point3Dfloat world4= new Point3Dfloat();
	Point3Dint voxel1= new Point3Dint();
	Point3Dint voxel2= new Point3Dint();
	Point3Dint voxel3= new Point3Dint();
	Point3Dint voxel4= new Point3Dint();
	common_sampling.voxel2world( world1, cs_max_dim0, cs_vox, cs_max_dim2);
	common_sampling.voxel2world( world2, cs_max_dim0, cs_vox, 0);
	common_sampling.voxel2world( world3, 0, cs_vox, cs_max_dim2);
	common_sampling.voxel2world( world4, 0, cs_vox, 0);
//System.out.println("\tworld1: "+world1+" world3: "+world3);
//System.out.println("\tworld2: "+world2+" world4: "+world4);
	this.world2voxel( voxel1, world1);
	this.world2voxel( voxel2, world2);
	this.world2voxel( voxel3, world3);
	this.world2voxel( voxel4, world4);
//System.out.println("\tvoxel1.y: "+voxel1.y+" voxel3.y: "+voxel3.y+";; voxel2.y: "+voxel2.y+" voxel4.y: "+voxel4.y);
	int min_y= Math.min( voxel1.y, voxel2.y);
	min_y= Math.min( min_y, voxel3.y);
	min_y= Math.min( min_y, voxel4.y);
	int max_y= Math.max( voxel1.y, voxel2.y);
	max_y= Math.max( max_y, voxel3.y);
	max_y= Math.max( max_y, voxel4.y);
//System.out.println("\tmin_y: "+min_y+" max_y:"+max_y+" return: "+((max_y-min_y)/2 + min_y));
//	common_sampling.voxel2world( world1, 121, cs_vox, 108);
//	this.world2voxel( voxel1, world1);
if (RETURN_0)
	return voxel4.y;
else if (RETURN_MID)
	return ((max_y-min_y)/2 + min_y);
else if (RETURN_MID_OFFSET)
	return ((max_y-min_y)/2 + min_y - 5);
else
	return voxel4.y;
    }
    private final int getThisZFromCommonZ( int cs_vox, int cs_max_dim0, int cs_max_dim1, VolumeHeader common_sampling) {
	Point3Dfloat world1= new Point3Dfloat();
	Point3Dfloat world2= new Point3Dfloat();
	Point3Dfloat world3= new Point3Dfloat();
	Point3Dfloat world4= new Point3Dfloat();
	Point3Dint voxel1= new Point3Dint();
	Point3Dint voxel2= new Point3Dint();
	Point3Dint voxel3= new Point3Dint();
	Point3Dint voxel4= new Point3Dint();
	common_sampling.voxel2world( world1, cs_max_dim0, cs_max_dim1, cs_vox);
	common_sampling.voxel2world( world2, cs_max_dim0, 0, cs_vox);
	common_sampling.voxel2world( world3, 0, cs_max_dim1, cs_vox);
	common_sampling.voxel2world( world4, 0, 0, cs_vox);
	this.world2voxel( voxel1, world1);
	this.world2voxel( voxel2, world2);
	this.world2voxel( voxel3, world3);
	this.world2voxel( voxel4, world4);
	int min_z= Math.min( voxel1.z, voxel2.z);
	min_z= Math.min( min_z, voxel3.z);
	min_z= Math.min( min_z, voxel4.z);
	int max_z= Math.max( voxel1.z, voxel2.z);
	max_z= Math.max( max_z, voxel3.z);
	max_z= Math.max( max_z, voxel4.z);
//	common_sampling.voxel2world( world4, 121, 166, cs_vox);
//	this.world2voxel( voxel1, world1);
if (RETURN_0)
	return voxel4.z;
else if (RETURN_MID)
	return ((max_z-min_z)/2 + min_z);
else if(RETURN_MID_OFFSET)
	return ((max_z-min_z)/2 + min_z - 16);
else
	return voxel4.z;
    }


    /**
     * @param voxel_value a 0..255 (byte) value
     * @return the image value (real value) corresponding to voxel_value
     */
    public final float voxel2image( short voxel_value) {
	return (float)voxel_value / 255f * (image_high - image_low) + image_low;
    }

    /* Note: short is used instead of the more logical byte merely for
	convenience (bytes are signed, so additional code is needed to
	interpret bytes as unsigned, i.e. 0...255) */
    /**
     * @param image_value (aka real value)
     * @return the 0..255 voxel value corresponding to image_value 
     */
    public final short image2voxel( float image_value) {
	return 
	    (short)Math.round( (image_value - image_low)/(image_high - image_low) * 255);
	// Math.round() returns its float argument rounded to the nearest
	// integral value. If the argument is equidistant from two integers,
	// the method returns the greater of the two integers. 
    }


    /* ** world2voxel ** 
     */

    /* Note: for supporting arbitrary cursor positions in the
       Slice2DViewport-s, we'll need an overloaded version of this
       method that returns the result as Point3Dfloat! */

    final public Point3Dint world2voxel( float x, float y, float z) {
	Point3Dint voxel;
	world2voxel( voxel= new Point3Dint(), x, y, z);
	return voxel;
    }

    final public void world2voxel( Point3Dint voxel, 
				   float wx, float wy, float wz) {
//voxel.x= Math.round( (wx - start_x) / step_x );
//voxel.y= Math.round( (wy - start_y) / step_y );
//voxel.z= Math.round( (wz - start_z) / step_z );

//System.out.println("world2voxel.."+Util.arrayToString(WORLD2VOX));
	voxel.x= Math.round(WORLD2VOX[0][0]*wx + WORLD2VOX[0][1]*wy + WORLD2VOX[0][2]*wz + WORLD2VOX[0][3]);
	voxel.y= Math.round(WORLD2VOX[1][0]*wx + WORLD2VOX[1][1]*wy + WORLD2VOX[1][2]*wz + WORLD2VOX[1][3]);
	voxel.z= Math.round(WORLD2VOX[2][0]*wx + WORLD2VOX[2][1]*wy + WORLD2VOX[2][2]*wz + WORLD2VOX[2][3]);

	if (DEBUG_WV) System.out.println("world2voxel - world: ("+wx+","+wy+","+wz+") voxel:"+voxel);
	// Math.round() returns its float argument rounded to the nearest
	// integral value. If the argument is equidistant from two integers,
	// the method returns the greater of the two integers. 
    }

    final public Point3Dint world2voxel( Point3Dfloat world) {
	return world2voxel( world.x, world.y, world.z);
    }

    final public void world2voxel( Point3Dint voxel, 
				   Point3Dfloat world) {
	world2voxel( voxel, world.x, world.y, world.z);
    }


    /* ** voxel2world ** 
     */

    final public Point3Dfloat voxel2world( int x, int y, int z) {
	Point3Dfloat world;
	voxel2world( world= new Point3Dfloat(), x, y, z);
	return world;
    }

    final public void voxel2world( Point3Dfloat world, 
				   int vx, int vy, int vz) {
//world.x= start_x + step_x * vx;
//world.y= start_y + step_y * vy;
//world.z= start_z + step_z * vz;

	world.x= VOX2WORLD[0][0]*vx + VOX2WORLD[0][1]*vy + VOX2WORLD[0][2]*vz + VOX2WORLD[0][3];
	world.y= VOX2WORLD[1][0]*vx + VOX2WORLD[1][1]*vy + VOX2WORLD[1][2]*vz + VOX2WORLD[1][3];
	world.z= VOX2WORLD[2][0]*vx + VOX2WORLD[2][1]*vy + VOX2WORLD[2][2]*vz + VOX2WORLD[2][3];

	if (DEBUG_WV) System.out.println("voxel2world - voxel: ("+vx+","+vy+","+vz+") world:"+world);
    }

    /** this version can be used to prevent loss of precision (roundup errors),
	where applicable or useful...
    */
    final public Point3Dfloat voxel2world( float x, float y, float z) {
	Point3Dfloat world;
	voxel2world( world= new Point3Dfloat(), x, y, z);
	return world;
    }

    /** this version can be used to prevent loss of precision (roundup errors),
	where applicable or useful...
    */
    final public void voxel2world( Point3Dfloat world, 
				   float vx, float vy, float vz) {
//world.x= start_x + step_x * vx;
//world.y= start_y + step_y * vy;
//world.z= start_z + step_z * vz;

	world.x= VOX2WORLD[0][0]*vx + VOX2WORLD[0][1]*vy + VOX2WORLD[0][2]*vz + VOX2WORLD[0][3];
	world.y= VOX2WORLD[1][0]*vx + VOX2WORLD[1][1]*vy + VOX2WORLD[1][2]*vz + VOX2WORLD[1][3];
	world.z= VOX2WORLD[2][0]*vx + VOX2WORLD[2][1]*vy + VOX2WORLD[2][2]*vz + VOX2WORLD[2][3];

	if (DEBUG_WV) System.out.println("voxel2world - voxel: ("+vx+","+vy+","+vz+") world:"+world);
    }

    final public Point3Dfloat voxel2world( Point3Dint voxel) {
	return voxel2world( voxel.x, voxel.y, voxel.z);
    }

    final public void voxel2world( Point3Dfloat world,
				   Point3Dint voxel) {
	voxel2world( world, voxel.x, voxel.y, voxel.z);
    }

    final public void voxel2world( Point3Dfloat world,
				   final int[] voxel) {
	voxel2world( world, voxel[0], voxel[1], voxel[2]);
    }



}

