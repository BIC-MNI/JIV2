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

import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.Hashtable;

/**
 * Functions for converting 3D positions between the "voxel" (array
 * index) and "world" coordinate systems.  All methods (functions) are
 * currently <code>static</code> because, for now, the coordinate
 * transformation is the same for all image volumes, and it doesn't
 * change after the initialization.
 *
 * Note: currently, this class is simply a dumb wrapper around the
 * methods from VolumeHeader -- it is only kept for convenience.
 *
 * @see VolumeHeader
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: CoordConv.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class CoordConv {

    /** shows which files are loaded */
    /*private*/ static final boolean    VERBOSE= true;

    /** for development only: see what's happening */
    /*private*/ static final boolean    DEBUG= false;
    /*private*/ static final boolean    DEBUG_INVERSE= false;
    /*private*/ static final boolean    DEBUG_VW= false;

    /** for development only: artificially delay the downloads */
    /*private*/ static final boolean DELAY_DOWNLOAD= false;

    /*private*/ static final int MATRIX_SIZE= 12;//support affine transforms only

    /*private*/ static VolumeHeader common_sampling;
    /*private*/ static VolumeHeader mni_sampling;
    /*private*/ static VolumeHeader native_sampling;

    /** coefficients for MNI2NAT transform */
    /*private*/ static float[] MNI2NAT;
    /** coefficients for NAT2MNI transform */
    /*private*/ static float[] NAT2MNI;
    /** coefficients for MNI2LABELS transform */
    /*private*/ static float[] MNI2LABELS;
    /** coefficients for LABELS2MNI transform */
    /*private*/ static float[] LABELS2MNI;


    /*private*/ static Object[][] label_array;
    /*private*/ static Hashtable labels;

    static final public void setSampling( VolumeHeader new_common_sampling,
					  VolumeHeader new_mni_sampling,
					  VolumeHeader new_native_sampling) {
	common_sampling= new VolumeHeader( new_common_sampling);
	if (new_mni_sampling != null) mni_sampling= new VolumeHeader( new_mni_sampling);
	if (new_native_sampling != null) native_sampling= new VolumeHeader( new_native_sampling);
    }


    static final public void setLabelXFM( URL configPath, String xfmFileName) 
		throws MalformedURLException, IOException {
	if (DEBUG) System.out.println("Reading labels2mni transform matrix \n\tfrom file: "+xfmFileName);
	LABELS2MNI= Util.readMatrix(configPath, xfmFileName);
	if (LABELS2MNI != null)
		MNI2LABELS= Util.invertMatrix(LABELS2MNI);
	if (DEBUG) System.out.println("LABELS2MNI:\n"+Util.arrayToString( LABELS2MNI));
	if (DEBUG) System.out.println("MNI2LABELS:\n"+Util.arrayToString( MNI2LABELS));
    }// end setLabelXFM


    static final public void setNativeXFM( URL configPath, String xfmFileName) 
		throws MalformedURLException, IOException {
	if (DEBUG) System.out.println("Reading native2mni transform matrix \n\tfrom file: "+xfmFileName);
	NAT2MNI= Util.readMatrix(configPath, xfmFileName);
	if (NAT2MNI != null)
		MNI2NAT= Util.invertMatrix(NAT2MNI);
	if (DEBUG) System.out.println("NAT2MNI:\n"+Util.arrayToString( NAT2MNI));
	if (DEBUG) System.out.println("MNI2NAT:\n"+Util.arrayToString( MNI2NAT));
    }// end setNativeXFM




    static final public void finalizeXFMs(float[] nat_dir_cosines,
					  float[] mni_dir_cosines,
					  float[] label_dir_cosines) {

	//Add direction cosines to NATIVE vs MNI transforms
	NAT2MNI= Util.addTxfmMatrixCosines(NAT2MNI, nat_dir_cosines, mni_dir_cosines);

	//Add direction cosines to LABELS vs MNI transforms
	LABELS2MNI= Util.addTxfmMatrixCosines(LABELS2MNI, label_dir_cosines, mni_dir_cosines);

    }// end finalizeNativeXFMs




//####

    /* ** world2voxel ** 
     */
    static final public Point3Dint world2voxel_common( float x, float y, float z) {
	if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_common - world: ("+x+","+y+","+z+") voxel: "+common_sampling.world2voxel( x, y, z));
        return common_sampling.world2voxel( x, y, z);
    }
    static final public Point3Dint world2voxel_mni( float x, float y, float z) {
	if (mni_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_mni - world: ("+x+","+y+","+z+") voxel: "+mni_sampling.world2voxel( x, y, z));
	        return mni_sampling.world2voxel( x, y, z);
	}else return new Point3Dint(0,0,0);
    }
    static final public Point3Dint world2voxel_nat( float x, float y, float z) {
	if (native_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_nat - world: ("+x+","+y+","+z+") voxel: "+native_sampling.world2voxel( x, y, z));
	        return native_sampling.world2voxel( x, y, z);
	}else return new Point3Dint(0,0,0);
    }

    static final public void world2voxel_common( Point3Dint voxel,
                                          float wx, float wy, float wz) {
        common_sampling.world2voxel( voxel, wx, wy, wz);
	if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_common - world: ("+wx+","+wy+","+wz+") voxel: "+voxel);
    }
    static final public void world2voxel_mni( Point3Dint voxel,
                                          float wx, float wy, float wz) {
	if (mni_sampling != null) {
	        mni_sampling.world2voxel( voxel, wx, wy, wz);
		if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_mni - world: ("+wx+","+wy+","+wz+") voxel: "+voxel);
	}else {
		voxel.x= 0;
		voxel.y= 0;
		voxel.z= 0;
	}
    }
    static final public void world2voxel_nat( Point3Dint voxel,
                                          float wx, float wy, float wz) {
	if (native_sampling != null) {
	        native_sampling.world2voxel( voxel, wx, wy, wz);
		if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_nat - world: ("+wx+","+wy+","+wz+") voxel: "+voxel);
	}else {
		voxel.x= 0;
		voxel.y= 0;
		voxel.z= 0;
	}
    }

    static final public Point3Dint world2voxel_common( Point3Dfloat world) {
	if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_common - world: "+world+" voxel: "+common_sampling.world2voxel( world));
        return common_sampling.world2voxel( world);
    }
    static final public Point3Dint world2voxel_mni( Point3Dfloat world) {
	if (mni_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_mni - world: "+world+" voxel: "+mni_sampling.world2voxel( world));
	        return mni_sampling.world2voxel( world);
	}else return new Point3Dint(0,0,0);
    }
    static final public Point3Dint world2voxel_nat( Point3Dfloat world) {
	if (native_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_nat - world: "+world+" voxel: "+native_sampling.world2voxel( world));
	        return native_sampling.world2voxel( world);
	}else return new Point3Dint(0,0,0);
    }

    static final public void world2voxel_common( Point3Dint voxel,
                                          Point3Dfloat world) {
        common_sampling.world2voxel( voxel, world);
	if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_common - world: "+world+" voxel: "+voxel);
    }
    static final public void world2voxel_mni( Point3Dint voxel,
                                          Point3Dfloat world) {
	if (mni_sampling != null) {
	        mni_sampling.world2voxel( voxel, world);
		if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_mni - world: "+world+" voxel: "+voxel);
	}else {
		voxel.x= 0;
		voxel.y= 0;
		voxel.z= 0;
	}
    }
    static final public void world2voxel_nat( Point3Dint voxel,
                                          Point3Dfloat world) {
	if (native_sampling != null) {
	        native_sampling.world2voxel( voxel, world);
		if (DEBUG_VW) System.out.println("\tCoordConv.world2voxel_nat - world: "+world+" voxel: "+voxel);
	}else {
		voxel.x= 0;
		voxel.y= 0;
		voxel.z= 0;
	}
    }




    /* **  voxel2world ** 
     */

    static final public Point3Dfloat voxel2world_common( int x, int y, int z) {
	if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_common - voxel: ("+x+","+y+","+z+") world: "+common_sampling.voxel2world( x, y, z));
        return common_sampling.voxel2world( x, y, z);
    }
    static final public Point3Dfloat voxel2world_mni( int x, int y, int z) {
	if (mni_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_mni - voxel: ("+x+","+y+","+z+") world: "+mni_sampling.voxel2world( x, y, z));
	        return mni_sampling.voxel2world( x, y, z);
	}else return new Point3Dfloat(0,0,0);
    }
    static final public Point3Dfloat voxel2world_nat( int x, int y, int z) {
	if (mni_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_nat - voxel: ("+x+","+y+","+z+") world: "+native_sampling.voxel2world( x, y, z));
	        return native_sampling.voxel2world( x, y, z);
	}else return new Point3Dfloat(0,0,0);
    }

    static final public void voxel2world_common( Point3Dfloat world,
                                          int vx, int vy, int vz) {
        common_sampling.voxel2world( world, vx, vy, vz);
	if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_common - voxel: ("+vx+","+vy+","+vz+") world: "+world);
    }
    static final public void voxel2world_mni( Point3Dfloat world,
                                          int vx, int vy, int vz) {
	if (mni_sampling != null) {
	        mni_sampling.voxel2world( world, vx, vy, vz);
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_mni - voxel: ("+vx+","+vy+","+vz+") world: "+world);
	}else {
		world.x= 0;
		world.y= 0;
		world.z= 0;
	}
    }
    static final public void voxel2world_nat( Point3Dfloat world,
                                          int vx, int vy, int vz) {
	if (native_sampling != null) {
	        native_sampling.voxel2world( world, vx, vy, vz);
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_nat - voxel: ("+vx+","+vy+","+vz+") world: "+world);
	}else {
		world.x= 0;
		world.y= 0;
		world.z= 0;
	}
    }

    static final public Point3Dfloat voxel2world_common( float x, float y, float z) {
	if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_common - voxel: ("+x+","+y+","+z+") world: "+common_sampling.voxel2world( x, y, z));
        return common_sampling.voxel2world( x, y, z);
    }
    static final public Point3Dfloat voxel2world_mni( float x, float y, float z) {
	if (mni_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_mni - voxel: ("+x+","+y+","+z+") world: "+mni_sampling.voxel2world( x, y, z));
	        return mni_sampling.voxel2world( x, y, z);
	}else return new Point3Dfloat(0,0,0);
    }
    static final public Point3Dfloat voxel2world_nat( float x, float y, float z) {
	if (native_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_nat - voxel: ("+x+","+y+","+z+") world: "+native_sampling.voxel2world( x, y, z));
	        return native_sampling.voxel2world( x, y, z);
	}else return new Point3Dfloat(0,0,0);
    }

    static final public void voxel2world_common( Point3Dfloat world,
                                          float vx, float vy, float vz) {
        common_sampling.voxel2world( world, vx, vy, vz);
	if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_common - voxel: ("+vx+","+vy+","+vz+") world: "+world);
    }
    static final public void voxel2world_mni( Point3Dfloat world,
                                          float vx, float vy, float vz) {
	if (mni_sampling != null) {
	        mni_sampling.voxel2world( world, vx, vy, vz);
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_mni - voxel: ("+vx+","+vy+","+vz+") world: "+world);
	}else {
		world.x= 0;
		world.y= 0;
		world.z= 0;
	}
    }
    static final public void voxel2world_nat( Point3Dfloat world,
                                          float vx, float vy, float vz) {
	if (native_sampling != null) {
	        native_sampling.voxel2world( world, vx, vy, vz);
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_nat - voxel: ("+vx+","+vy+","+vz+") world: "+world);
	}else {
		world.x= 0;
		world.y= 0;
		world.z= 0;
	}
    }

    static final public Point3Dfloat voxel2world_common( Point3Dint voxel) {
	if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_common - voxel: "+voxel+" world: "+common_sampling.voxel2world( voxel));
        return common_sampling.voxel2world( voxel);
    }
    static final public Point3Dfloat voxel2world_mni( Point3Dint voxel) {
	if (mni_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_mni - voxel: "+voxel+" world: "+mni_sampling.voxel2world( voxel));
	        return mni_sampling.voxel2world( voxel);
	}else return new Point3Dfloat(0,0,0);
    }
    static final public Point3Dfloat voxel2world_nat( Point3Dint voxel) {
	if (native_sampling != null) {
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_native - voxel: "+voxel+" world: "+native_sampling.voxel2world( voxel));
	        return native_sampling.voxel2world( voxel);
	}else return new Point3Dfloat(0,0,0);
    }

    static final public void voxel2world_common( Point3Dfloat world,
                                          Point3Dint voxel) {
        common_sampling.voxel2world( world, voxel);
	if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_common - voxel: "+voxel+" world: "+world);
    }
    static final public void voxel2world_mni( Point3Dfloat world,
                                          Point3Dint voxel) {
	if (mni_sampling != null) {
	        mni_sampling.voxel2world( world, voxel);
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_mni - voxel: "+voxel+" world: "+world);
	}else {
		world.x= 0;
		world.y= 0;
		world.z= 0;
	}
    }
    static final public void voxel2world_nat( Point3Dfloat world,
                                          Point3Dint voxel) {
	if (native_sampling != null) {
	        native_sampling.voxel2world( world, voxel);
		if (DEBUG_VW) System.out.println("\tCoordConv.voxel2world_nat - voxel: "+voxel+" world: "+world);
	}else {
		world.x= 0;
		world.y= 0;
		world.z= 0;
	}
    }


    /* ** mni2native ** 
     */
    static final public void mni2native( Point3Dfloat native_coord, Point3Dfloat mni) {
	if (MNI2NAT == null) {
		native_coord.x = native_coord.y = native_coord.z = 0;
		return;
	}
        native_coord.x= mni.x*MNI2NAT[0] + mni.y*MNI2NAT[1] + mni.z*MNI2NAT[2] + MNI2NAT[3];
        native_coord.y= mni.x*MNI2NAT[4] + mni.y*MNI2NAT[5] + mni.z*MNI2NAT[6] + MNI2NAT[7];
        native_coord.z= mni.x*MNI2NAT[8] + mni.y*MNI2NAT[9] + mni.z*MNI2NAT[10] + MNI2NAT[11];
    }
    static final public Point3Dfloat mni2native( Point3Dfloat mni) {
        Point3Dfloat native_coords;
        mni2native( native_coords= new Point3Dfloat(), mni);
        return native_coords;
    }
    static final public Point3Dfloat mni2native( float mni_x, float mni_y, float mni_z) {
        Point3Dfloat native_coord = new Point3Dfloat(0,0,0);
        native_coord.x= mni2native_X(mni_x,mni_y,mni_z);
        native_coord.y= mni2native_Y(mni_x,mni_y,mni_z);
        native_coord.z= mni2native_Z(mni_x,mni_y,mni_z);
        return native_coord;
    }


    static final public float mni2native_X( float mni_x, float mni_y, float mni_z) {
	if (MNI2NAT == null)
		return 0;
        return mni_x*MNI2NAT[0] + mni_y*MNI2NAT[1] + mni_z*MNI2NAT[2] + MNI2NAT[3];
    }
    static final public float mni2native_Y( float mni_x, float mni_y, float mni_z) {
	if (MNI2NAT == null)
		return 0;
        return mni_x*MNI2NAT[4] + mni_y*MNI2NAT[5] + mni_z*MNI2NAT[6] + MNI2NAT[7];
    }
    static final public float mni2native_Z( float mni_x, float mni_y, float mni_z) {
	if (MNI2NAT == null)
		return 0;
        return mni_x*MNI2NAT[8] + mni_y*MNI2NAT[9] + mni_z*MNI2NAT[10] + MNI2NAT[11];
    }



    /* ** native2mni ** 
     */
    static final public void native2mni( Point3Dfloat mni, 
					  Point3Dfloat native_coord) {
	if (NAT2MNI == null) {
		mni.x = mni.y = mni.z = 0;
		return;
	}
	mni.x= native_coord.x*NAT2MNI[0] + native_coord.y*NAT2MNI[1] + native_coord.z*NAT2MNI[2] + NAT2MNI[3];
	mni.y= native_coord.x*NAT2MNI[4] + native_coord.y*NAT2MNI[5] + native_coord.z*NAT2MNI[6] + NAT2MNI[7];
	mni.z= native_coord.x*NAT2MNI[8] + native_coord.y*NAT2MNI[9] + native_coord.z*NAT2MNI[10] + NAT2MNI[11];
    }
    static final public Point3Dfloat native2mni( Point3Dfloat native_coords) {
        Point3Dfloat mni;
        native2mni( mni= new Point3Dfloat(), native_coords);
        return mni;
    }





    /* ** mni2labels ** 
     */
    static final public void mni2labels( LabelCoords position_labels, 
					  Point3Dfloat mni_world) {
	if (MNI2LABELS == null) {
		position_labels.lat = position_labels.post = position_labels.sup = 0;
		return;
	}
	position_labels.lat= mni_world.x*MNI2LABELS[0] + mni_world.y*MNI2LABELS[1] + mni_world.z*MNI2LABELS[2] + MNI2LABELS[3];
	position_labels.post= mni_world.x*MNI2LABELS[4] + mni_world.y*MNI2LABELS[5] + mni_world.z*MNI2LABELS[6] + MNI2LABELS[7];
	position_labels.sup= mni_world.x*MNI2LABELS[8] + mni_world.y*MNI2LABELS[9] + mni_world.z*MNI2LABELS[10] + MNI2LABELS[11];
    }
    static final public LabelCoords mni2labels( Point3Dfloat mni_world) {
        LabelCoords position_labels;
        mni2labels( position_labels= new LabelCoords(), mni_world);
        return position_labels;
    }

    /* ** labels2mni ** 
     */
    static final public void labels2mni( Point3Dfloat mni_world, 
					  LabelCoords position_labels) {
	if (LABELS2MNI == null) {
		mni_world.x = mni_world.y = mni_world.z = 0;
		return;
	}
	mni_world.x= position_labels.lat*LABELS2MNI[0] + position_labels.post*LABELS2MNI[1] + position_labels.sup*LABELS2MNI[2] + LABELS2MNI[3];
	mni_world.y= position_labels.lat*LABELS2MNI[4] + position_labels.post*LABELS2MNI[5] + position_labels.sup*LABELS2MNI[6] + LABELS2MNI[7];
	mni_world.z= position_labels.lat*LABELS2MNI[8] + position_labels.post*LABELS2MNI[9] + position_labels.sup*LABELS2MNI[10] + LABELS2MNI[11];
    }


    /* ** intensity2label ** 
     */
    static final public String intensity2label( int intensity) {
	if (intensity <= 0 || intensity >= 254)
		return "no_label";
	String label;
	try {
		label= ( labels.get( new Float(intensity))).toString();
	}catch (NullPointerException e) {
		return "unknown intensity: "+intensity; }
	if (DEBUG) return label+" ("+intensity+")";
	return label;
    }


    static final public void setLabelMapping( URL configPath, String labelFileName)
		throws MalformedURLException, IOException {
	// "/path_to_data/paxinos-MNI_label_mapping.txt"
	URL labelMappingPathURL= new URL( configPath, labelFileName);

	//Read label mapping
//	try {
		if (DEBUG) System.out.println("Reading label mapping \n\tfrom file: "+labelFileName);
		label_array=readLabelMapping(labelMappingPathURL, labelFileName);
//	}
//	catch (IOException ioe){
//		System.out.println("Error reading from label mapping file: "+labelFileName);
//		label_array= null;
//	}

	labels= new Hashtable();
	if (label_array != null) {
		for( int i= 0; i < label_array.length; ++i)
			labels.put( label_array[i][0], label_array[i][1]);
	}
    }// end setLabelMapping




    static final Object[][] readLabelMapping( URL source_url, String labelMapping_filename) throws IOException {
	Object[][] label_array;
	
	InputStream input_stream= null;
//	try {
		input_stream= Util.openURL( source_url);
		BufferedReader br = new BufferedReader(new InputStreamReader(input_stream));
		String strLine;
		//Initialize the matrix:
		int num_of_labels= 0;
		if ((strLine = br.readLine()) != null){
			if (strLine.startsWith("#"))
				num_of_labels= Short.parseShort(strLine.substring(1));
		}
		if (DEBUG) System.out.println("num_of_labels: "+num_of_labels);
		label_array= new Object [num_of_labels][2];
		int i= 0;
		//Read correspondences in
		while ((strLine = br.readLine()) != null){
			StringTokenizer num_tokens= new StringTokenizer( strLine, " ", false);
			if( !num_tokens.hasMoreTokens())
				throw new IOException("No intensity on line #"+(i+2));
			String str_intensity= num_tokens.nextToken();
			if( !num_tokens.hasMoreTokens())
				throw new IOException("No label on line #"+(i+2));
			String str_label= num_tokens.nextToken();
			if( num_tokens.hasMoreTokens())
				throw new IOException("Extra data on line #"+(i+2));
			if (i >= num_of_labels)
				throw new IOException ("Expected "+num_of_labels+" label entries but found more.");
			label_array[i][0]= Float.valueOf(str_intensity);
			label_array[i][1]= str_label;
///###			if (DEBUG) System.out.println("["+i+"] "+label_array[i][0]+": "+label_array[i][1]);
			i++;
		}// end while readLine != null
		
		if (i != num_of_labels)
			throw new IOException ("Expected "+num_of_labels+" label entries but only found "+i+".");

		//Close reader
		br.close();
		input_stream.close();
//	}// end try
//	catch(FileNotFoundException e) {
//		System.out.println( e);
//		return null;
//	}
//	catch(IOException e) {
//		throw IOException("Warning! Problem reading the label file: "+labelMapping_filename+"\n\t" + e);
//		return null;
//	}
	if (VERBOSE) System.out.println( source_url + " loading done!");
	return label_array;
    }// end readLabelMapping


/*

    // The full transforms, according to .xfm
    static float[] MNI2NAT_Full = new float[]{0.987148243425476f,  0.13492280739352f,  -0.0283611586204781f, 1.89454504110754f,
					     -0.14688364170279f,   0.911643595421521f, -0.113838406449514f, 66.830568778305f,
					      0.0329577194156644f, 0.117246413789787f,  1.00862015559051f,  17.5784751676145f};
    static float[] NAT2MNI_Full = new float[]{0.990618526935577f,  -0.148044526576996f, 0.0111458571627736f,  7.82120132446289f,
					      0.153340190649033f,   1.05830907821655f,  0.123758308589458f, -73.1933898925781f,
					     -0.0501944310963154f, -0.118184961378574f, 0.976703107357025f,  -9.17548751831055f};
    static float[] MNI2LABELS_Full = new float[]{+1.13240f, +0.01884f, +0.01824f,  +1.67555f,
						-0.14023f, +1.03486f, +0.06359f,  -3.55865f,
						+0.01787f, +0.03466f, +1.04026f, +17.04690f};
    static float[] LABELS2MNI_Full = new float[]{+0.88138f, -0.01556f, -0.01450f,  -1.2849f,
						+0.12061f, +0.96617f, -0.06118f,  +4.2790f,
						-0.01916f, -0.03193f, +0.96358f, -16.5076f};
    // FOR TESTING ROUGH REGISTRATION (NO ROTATION)
    static float[] MNI2NAT_Trans = new float[]{1f, 0f, 0f, -8f,
					       0f, 1f, 0f, 73f,
					       0f, 0f, 1f,  9f};
    static float[] NAT2MNI_Trans = new float[]{1f, 0f, 0f,   8f,
					       0f, 1f, 0f, -73f,
					       0f, 0f, 1f,  -9f};
    static float[] MNI2LABELS_Trans = new float[]{1f, 0f, 0f,   2f,
						 0f, 1f, 0f,  -4f,
						 0f, 0f, 1f,  17f};
    static float[] LABELS2MNI_Trans = new float[]{1f, 0f, 0f,  -2f,
						 0f, 1f, 0f,   4f,
						 0f, 0f, 1f, -17f};
    // FOR TESTING TEXTFIELD UPDATES
    static float[] MNI2NAT_Test = new float[]{1f, 0f, 0f, 10f,
					      0f, 1f, 0f, 10f,
					      0f, 0f, 1f, 10f};
    static float[] NAT2MNI_Test = new float[]{1f, 0f, 0f, -10f,
					      0f, 1f, 0f, -10f,
					      0f, 0f, 1f, -10f};
    static float[] MNI2LABELS_Test = new float[]{0f, 2f, 0f, 0f,
						2f, 0f, 0f, 0f,
					 	2f, 2f, 1f, 0f};
    static float[] LABELS2MNI_Test = new float[]{ 0f, .5f, 0f, 0f,
						.5f,  0f, 0f, 0f,
						-1f, -1f, 1f, 0f};
    // FOR TESTING WITHOUT DISTRACTIONS
    static float[] IDENTITY = new float[]{1f, 0f, 0f, 0f,
					  0f, 1f, 0f, 0f,
					  0f, 0f, 1f, 0f};


*/

}
