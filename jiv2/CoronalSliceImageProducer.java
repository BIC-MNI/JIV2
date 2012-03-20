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

import java.awt.image.*;

/**
 * A <code>SliceImageProducer</code> customized for "coronal"
 * (Y=constant) 2D slices.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: CoronalSliceImageProducer.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class CoronalSliceImageProducer extends SliceImageProducer {

    public CoronalSliceImageProducer( Data3DVolume data_volume, 
				      Point3Dfloat world,
				      int default_slice, 
				      IndexColorModel default_colormap,
				      boolean isNative ) {

	super( default_slice, 
	       world,
	       new byte[ data_volume.getXSize() * data_volume.getZSize()],
	       data_volume.getXSize(), 
	       data_volume.getZSize(),
	       default_colormap,
	       data_volume,
	       isNative);
	_getNewSliceData( true);
    }

    // NB: any changes here should also be made 
    // in the other two subclasses of SliceImageProducer!

    public final int getMaxSliceNumber() { return data_volume.getYSize()-1; }

    public final float getOrthoStep() { return data_volume.getYStep(); }

    synchronized public final void positionChangeDetected( PositionEvent e) {

	if (DEBUG_TRACE) System.out.println("\t\t\t\t*CorIP.positionChangeDetected");

	if (DEBUG) {
		System.out.print("\n** CoronalSliceImageProducer->positionChangeDetected");
		if (DEBUG_HIGH) System.out.println(" with:\n\t"+e);
		else System.out.println();
		System.out.println("\tat: "+this);
	}

	float old_world_x;
	float new_world_y;
	float old_world_z;
	Point3Dint new_voxel; // for converting new_world_y to voxel/slice_coords

	//If mask doesn't indicate a new slice is needed, do nothing
	if (! (e.isYmniChanged() || e.isYnatChanged())) { 
	    if (DEBUG_TRACE) System.out.println("\t\t\t\t*CorIP.positionChangeDetected DONE!\n");
	    return;
	}
	//Otherwise, get new slice (in world space)
	if (!isNative) {
	    old_world_x= e.getXmni();
	    new_world_y= e.getYmni();
	    old_world_z= e.getZmni();
	}
	else {
	    old_world_x= e.getXnat();
	    new_world_y= e.getYnat();
	    old_world_z= e.getZnat();
	}

	//Convert world to voxel, which is actually the same as the slice #.
	if (DEBUG_A) System.out.println("4 world2voxel (common or native) to determine new slice image");
	if (DEBUG_TRACE) System.out.println("\t\t\t\t**CorIP.positionChangeDetected -> CoordConv.world2voxel_common/nat");
	if (!isNative)
		new_voxel= CoordConv.world2voxel_common( old_world_x, new_world_y, old_world_z);
	else
		new_voxel= CoordConv.world2voxel_nat( old_world_x, new_world_y, old_world_z);

	// don't update the image if we don't have to...
	// although the world_y coordinate may have changed,
	// it may not be changed enough to translate into a different slice
	if( crt_slice == new_voxel.y) {
	    if (DEBUG) System.out.println("no new slice needed because new_slice is "+new_voxel.y);
	    if (DEBUG_TRACE) System.out.println("\t\t\t\t*CorIP.positionChangeDetected DONE!\n");
	    return;
	}

	// update the displayed slice:
	if( DEBUG_HIGH) System.out.println("\tTherefore, new y: "+new_world_y+" [crt_slice:"+new_voxel.y+"]");
	crt_slice= new_voxel.y;
	world.x= old_world_x;
	world.y= new_world_y;
	world.z= old_world_z;
	if (DEBUG_TRACE) System.out.println("\t\t\t\t**CorIP.positionChangeDetected -> !_getNewSliceData\n");
	_getNewSliceData( true);

	if (DEBUG_TRACE) System.out.println("\t\t\t\t*CorIP.positionChangeDetected DONE!\n");
    }// end positionChangeDetected(e)

    /*private*/ final void _getNewSliceData( boolean future_notification) {

	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t!_getNewSliceData");

	// reuse the existing slice_data array!
	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t!!_getNewSliceData -> ^data_volume.getCoronalSlice()");
	data_volume.getCoronalSlice( world, crt_slice, slice_data, 
				     future_notification ? this : null );

	// Send another frame (i.e. update the image);
	// this version of MemoryImageSource::newPixels() will send the
	// data presently found in 'slice_data' (it stores a ref internally)
	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t!!_getNewSliceData -> newPixels()");
	newPixels();

	if (DEBUG_TRACE) System.out.println("\t\t\t\t\t!_getNewSliceData DONE!\n");
    }

}
