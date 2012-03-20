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
 * A <code>Slice2DViewport</code> customized for "transverse"
 * (Z=constant) slices.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: TransverseSlice2DViewport.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class TransverseSlice2DViewport extends Slice2DViewport {

    protected static final boolean DEBUG= false;

    // NB: any changes in this class should also be made
    // in the other two subclasses of Slice2DViewport !

    public TransverseSlice2DViewport( ImageProducer ip, VolumeHeader local_sampling,
				      String title,
				      PositionListener pos_listener_for_ip,
				      Point3Dfloat initial_cursor_mni,
				      Point3Dfloat initial_cursor_nat,
				      boolean isNative ) {
	super( ip, local_sampling, title, pos_listener_for_ip,
	       // no re-ordering of coordinates necessary for 'transverse'
	       new Point3Dfloat( initial_cursor_mni),
	       new Point3Dfloat( initial_cursor_nat),
	       isNative );
    }

    // the "translation between the internal and external x,y,z is
    // very easy for transverse slices: they coincide! hurray!

    synchronized final public void positionChangeDetected( PositionEvent e ) {

	if (DEBUG_TRACE) System.out.println("\t\t\t\t*TransVP.positionChangeDetected");

	if (DEBUG) {
		System.out.print("\n** TransverseSlice2DViewport->positionChangeDetected");
	        if (DEBUG_HIGH) System.out.println(" with e:\n"+e);
	        else System.out.println();
		System.out.println("\tat: "+this.toString());
	}

	// NB: this assumes that our ImageProducer also received this
	// event, and will update the supplied image accordingly...
	if (!isNative && e.isZmniChanged()) {
		if (DEBUG) System.out.println("Therefore, new slice (z): " + e.getZmni());
		_local_cursor_setZ( e.getZmni());
	}
	else if (isNative && e.isZnatChanged()) {
		if (DEBUG) System.out.println("Therefore, new slice (z): " + e.getZnat());
		_local_cursor_setZ( e.getZnat());
	}

	if (!isNative && ( e.isXmniChanged() || e.isYmniChanged()) ) {
		if (DEBUG) System.out.println("Also, new xy: (" + e.getXmni() + "," + e.getYmni()+")");
		if (DEBUG_TRACE) System.out.println("\t\t\t\t**TransVP.positionChangeDetected -> %_newCursor");
		_newCursor( e.isXmniChanged() ? e.getXmni() : _local_cursor_getX(), 
			e.isYmniChanged() ? e.getYmni() : _local_cursor_getY(), 
			e.isZmniChanged() ? e.getZmni() : _local_cursor_getZ(), 
			false);
	}
	else if (isNative && ( e.isXnatChanged() || e.isYnatChanged()) ) {
		if (DEBUG) System.out.println("Also, new xy_nat: (" + e.getXnat() + "," + e.getYnat()+")");
		if (DEBUG_TRACE) System.out.println("\t\t\t\t**TransVP.positionChangeDetected -> %_newCursor");
		_newCursor( e.isXnatChanged() ? e.getXnat() : _local_cursor_getX(), 
			e.isYnatChanged() ? e.getYnat() : _local_cursor_getY(), 
			e.isZnatChanged() ? e.getZnat() : _local_cursor_getZ(), 
			false);
	}

	if (DEBUG_TRACE) System.out.println("\t\t\t\t*TransVP.positionChangeDetected DONE!\n");
    } //end positionChangeDetected()

    /** This method is only called when this viewport is the originator
         of a position_change. This can happen by _newCursor(int int) - clicking on a vi$
         or, by _newSlice(int) - holding button on view to scroll.
        This means that local_changed_coords_mask is always either 3, 4, 24, or 32 :) */
    /** local_changed_coords_mask can either be in MNI or NATIVE coords!!
	But it is always rotated to local coordinates */
    final protected void _firePositionEvent( int local_changed_coords_mask) {

	if (DEBUG_TRACE) System.out.println("\t\t$TransVP._firePositionEvent() from $$_newCursor() or _newSlice()");

	//No need to unscramble mask for 'transverse' case
	int world_change_mask= local_changed_coords_mask;

        //again, no need to re-order coordinates for 'transverse':
        Point3Dfloat global_cursor= _local_cursor_get();

	//Finally, sync cursors:
        Point3Dfloat global_cursor_mni;
        Point3Dfloat global_cursor_nat;
        if(!isNative) {
                global_cursor_mni= global_cursor;
                global_cursor_nat= CoordConv.mni2native(global_cursor_mni); }
        else {
                global_cursor_nat= global_cursor;
                global_cursor_mni= CoordConv.native2mni(global_cursor_nat); }

	if (DEBUG) {
		System.out.println("TransverseSlice2DViewport->_firePositionEvent "+_local_cursor_get());
		System.out.println("\tworld_change_mask: "+world_change_mask);
		System.out.println("\tmni_cursor:"+global_cursor_mni);
		System.out.println("\tnat_cursor:"+global_cursor_nat);
	}

        //Now start to fire the PositionEvent
	if (DEBUG_TRACE) System.out.println("\t\t$$TransVP._firePositionEvent -> __aid_to_firePositionEvent()");
        __aid_to_firePositionEvent( new PositionEvent( this,
						       isNative ? PositionEvent.NAT_EVENT : PositionEvent.MNI_EVENT,
                                                       world_change_mask,
                                                       global_cursor_mni,
                                                       global_cursor_nat,
						       CoordConv.mni2labels(global_cursor_mni) ));
	if (DEBUG_TRACE) System.out.println("\t\t$TransVP._firePositionEvent() DONE!\n");
    }

    final protected void _voxel2world( Point3Dfloat world, 
				       float vx, float vy, float vz) {
	// no re-ordering of coordinates necessary for 'transverse'
	if (!isNative) {
		if (DEBUG_VW) System.out.println("TransSlice2DVP -> CoordConv.voxel2world_common");
		CoordConv.voxel2world_common( world, vx, vy, vz);
	}
	else {
		if (DEBUG_VW) System.out.println("TransSlice2DVP -> CoordConv.voxel2world_nat");
		CoordConv.voxel2world_nat( world, vx, vy, vz);
	}
    }

    final protected void _world2voxel( Point3Dint voxel,
				       float wx, float wy, float wz) {
	// no re-ordering of coordinates necessary for 'transverse'
	if (!isNative) {
		if (DEBUG_VW) System.out.println("TransSlice2DVP -> CoordConv.world2voxel_common");
		CoordConv.world2voxel_common( voxel, wx, wy, wz);
	}
	else {
		if (DEBUG_VW) System.out.println("TransSlice2DVP -> CoordConv.world2voxel_nat");
		CoordConv.world2voxel_nat( voxel, wx, wy, wz);
	}
    }

    final protected void _world2voxel( Point3Dint voxel,
				       Point3Dfloat world) {
	// no re-ordering of coordinates necessary for 'transverse'
	if (!isNative) {
		if (DEBUG_VW) System.out.println("TransSlice2DVP -> CoordConv.world2voxel_common");
		CoordConv.world2voxel_common( voxel, world);
	}
	else {
		if (DEBUG_VW) System.out.println("TransSlice2DVP -> CoordConv.world2voxel_nat");
		CoordConv.world2voxel_nat( voxel, world);
	}
    }
}
