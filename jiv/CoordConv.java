
// $Id: CoordConv.java,v 1.1 2001-04-08 00:04:27 cc Exp $
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

/**
 * Functions for converting 3D positions between the "voxel" (array
 * index) and "world" coordinate systems.  All methods (functions) are
 * currently <code>static</code> because, for now, the coordinate
 * transformation is the same for all image volumes, and it doesn't
 * change after the initialization.
 *
 * @author Chris Cocosco (crisco@bic.mni.mcgill.ca)
 * @version $Id: CoordConv.java,v 1.1 2001-04-08 00:04:27 cc Exp $ 
 */
public final class CoordConv {

    /* ** world2voxel ** 
     */

    /* Note: for supporting arbitrary cursor positions in the
       Slice2DViewport-s, we'll need an overridden version of this
       method that returns the result as Point3Dfloat! */

    static final public Point3Dint world2voxel( float x, float y, float z) {
	Point3Dint voxel;
	world2voxel( voxel= new Point3Dint(), x, y, z);
	return voxel;
    }

    static final public void world2voxel( Point3Dint voxel, 
					  float wx, float wy, float wz) {
	voxel.x= Math.round( wx + 90F);
	voxel.y= Math.round( wy + 126F);
	voxel.z= Math.round( wz + 72F);
	// Math.round() returns its float argument rounded to the nearest
	// integral value. If the argument is equidistant from two integers,
	// the method returns the greater of the two integers. 
    }

    static final public Point3Dint world2voxel( Point3Dfloat world) {
	return world2voxel( world.x, world.y, world.z);
    }

    static final public void world2voxel( Point3Dint voxel, 
					  Point3Dfloat world) {
	world2voxel( voxel, world.x, world.y, world.z);
    }

    /* ** voxel2world ** 
     */

    static final public Point3Dfloat voxel2world( int x, int y, int z) {
	Point3Dfloat world;
	voxel2world( world= new Point3Dfloat(), x, y, z);
	return world;
    }

    static final public void voxel2world( Point3Dfloat world, 
					  int vx, int vy, int vz) {
	world.x= -90 + vx;
	world.y= -126 + vy;
	world.z= -72 + vz;
    }

    /** this version can be used to prevent loss of precision (roundup errors),
	where applicable or useful...
    */
    static final public Point3Dfloat voxel2world( float x, float y, float z) {
	Point3Dfloat world;
	voxel2world( world= new Point3Dfloat(), x, y, z);
	return world;
    }

    /** this version can be used to prevent loss of precision (roundup errors),
	where applicable or useful...
    */
    static final public void voxel2world( Point3Dfloat world, 
					  float vx, float vy, float vz) {
	world.x= -90F + vx;
	world.y= -126F + vy;
	world.z= -72F + vz;
    }

    static final public Point3Dfloat voxel2world( Point3Dint voxel) {
	return voxel2world( voxel.x, voxel.y, voxel.z);
    }

    static final public void voxel2world( Point3Dfloat world,
					  Point3Dint voxel) {
	voxel2world( world, voxel.x, voxel.y, voxel.z);
    }
}
