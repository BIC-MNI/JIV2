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
 * A <code>Slice2DViewport</code> customized for "coronal"
 * (Y=constant) slices.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: CoronalSlice2DViewport.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class CoronalSlice2DViewport extends Slice2DViewport {

    // NB: any changes in this class should also be made
    // in the other two subclasses of Slice2DViewport !

    public CoronalSlice2DViewport( ImageProducer ip, 
				   VolumeHeader local_sampling,
				   String title,
				   PositionListener pos_listener_for_ip,
				   Point3Dfloat initial_cursor_mni,
				   Point3Dfloat initial_cursor_nat,
				   boolean isNative ) {
	super( ip, local_sampling, title, pos_listener_for_ip, 
	       new Point3Dfloat( initial_cursor_mni.x, // vport horiz
				 initial_cursor_mni.z, // vport vert
				 initial_cursor_mni.y ), // ortho to vport
	       new Point3Dfloat( initial_cursor_nat.x, // vport horiz
				 initial_cursor_nat.z, // vport vert
				 initial_cursor_nat.y ), // ortho to vport
		isNative   );
    }

    synchronized final public void positionChangeDetected( PositionEvent e ) {

	if (DEBUG_TRACE) System.out.println("\t\t\t\t*CorVP.positionChangeDetected");

        if (DEBUG) {
		System.out.print("\n** CoronalSlice2DViewport->positionChangeDetected");
	        if (DEBUG_HIGH) System.out.println(" with e:\n"+e);
	        else System.out.println();
		System.out.println("\tat: "+this.toString());
	}

	//CHECK if the slice range is valid
	// NB: this assumes that our ImageProducer also received this
	// event, and will update the supplied image accordingly...
	if (!isNative && e.isYmniChanged()) {
		if (DEBUG) System.out.println("Therefore, new slice (y): " + e.getYmni());
		_local_cursor_setZ( e.getYmni());
	}
	else if (isNative && e.isYnatChanged()) {
		if (DEBUG) System.out.println("Therefore, new slice (y): " + e.getYnat());
		_local_cursor_setZ( e.getYnat());
	}

	if (!isNative && ( e.isXmniChanged() || e.isZmniChanged()) ) {
		if (DEBUG) System.out.println("Also, new xz: (" + e.getXmni() + "," + e.getZmni()+")");
		if (DEBUG_TRACE) System.out.println("\t\t\t\t**CorVP.positionChangeDetected -> %_newCursor");
		_newCursor( e.isXmniChanged() ? e.getXmni() : _local_cursor_getX(), 
			e.isZmniChanged() ? e.getZmni() : _local_cursor_getY(), 
			e.isYmniChanged() ? e.getYmni() : _local_cursor_getZ(), 
			false);
	}
	else if (isNative && ( e.isXnatChanged() || e.isZnatChanged()) ) {
		if (DEBUG) System.out.println("Also, new xz_nat: (" + e.getXnat() + "," + e.getZnat()+")");
		if (DEBUG_TRACE) System.out.println("\t\t\t\t**CorVP.positionChangeDetected -> %_newCursor");
		_newCursor( e.isXnatChanged() ? e.getXnat() : _local_cursor_getX(), 
			e.isZnatChanged() ? e.getZnat() : _local_cursor_getY(), 
			e.isYnatChanged() ? e.getYnat() : _local_cursor_getZ(), 
			false);
	}
	if (DEBUG_TRACE) System.out.println("\t\t\t\t*CorVP.positionChangeDetected DONE!\n");
    } //end positionChangeDetected()

    /** This method is only called when this viewport is the originator
         of a position_change. This can happen by _newCursor(int int) - clicking on a vi$
         or, by _newSlice(int) - holding button on view to scroll.
        This means that local_changed_coords_mask is always either 3, 4, 24, or 32 :) */
    /** local_changed_coords_mask can either be in MNI or NATIVE coords!!
        But it is always rotated to local coordinates */
    final protected void _firePositionEvent( final int local_changed_coords_mask) {

	if (DEBUG_TRACE) System.out.println("\t\t$CorVP._firePositionEvent() from %%_newCursor() or _newSlice()");

	//First unscramble mask
	int world_change_mask= 0;
	boolean isXmniLocalChanged= ( 0 != ( local_changed_coords_mask & PositionEvent.X_MNI));
	boolean isYmniLocalChanged= ( 0 != ( local_changed_coords_mask & PositionEvent.Y_MNI));
	boolean isZmniLocalChanged= ( 0 != ( local_changed_coords_mask & PositionEvent.Z_MNI));
	boolean isXnatLocalChanged= ( 0 != ( local_changed_coords_mask & PositionEvent.X_NAT));
	boolean isYnatLocalChanged= ( 0 != ( local_changed_coords_mask & PositionEvent.Y_NAT));
	boolean isZnatLocalChanged= ( 0 != ( local_changed_coords_mask & PositionEvent.Z_NAT));
	if (!isNative) {
	    if( isXmniLocalChanged)
		world_change_mask |= PositionEvent.X_MNI;
	    if( isYmniLocalChanged)
		world_change_mask |= PositionEvent.Z_MNI;
	    if( isZmniLocalChanged)
		world_change_mask |= PositionEvent.Y_MNI;
	}
	else {
	    if( isXnatLocalChanged)
		world_change_mask |= PositionEvent.X_NAT;
	    if( isYnatLocalChanged)
		world_change_mask |= PositionEvent.Z_NAT;
	    if( isZnatLocalChanged)
		world_change_mask |= PositionEvent.Y_NAT;
	}

	//Now unscramble cursor
        Point3Dfloat global_cursor= new Point3Dfloat( _local_cursor_getX(),
                                                      _local_cursor_getZ(),
                                                      _local_cursor_getY());
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
		System.out.println("CoronalSlice2DViewport->_firePositionEvent ("+_local_cursor_get()+")");
		System.out.println("\tlocal_change_mask: "+local_changed_coords_mask);
		System.out.println("\tmeans local_xyz_mni have changed: "+isXmniLocalChanged+","+isYmniLocalChanged+","+isZmniLocalChanged);
		System.out.println("\tmeans local_xyz_nat have changed: "+isXnatLocalChanged+","+isYnatLocalChanged+","+isZnatLocalChanged);
		System.out.println("\tworld_change_mask: "+world_change_mask);
		System.out.println("\tmni_cursor:"+global_cursor_mni);
		System.out.println("\tnat_cursor:"+global_cursor_nat);
	}

	//Now start to fire the PositionEvent
	if (DEBUG_TRACE) System.out.println("\t\t$$CorVP._firePositionEvent -> @__aid_to_firePositionEvent()");
	__aid_to_firePositionEvent( new PositionEvent( this,
                                                       isNative ? PositionEvent.NAT_EVENT : PositionEvent.MNI_EVENT,
						       world_change_mask, 
						       global_cursor_mni,
						       global_cursor_nat,
						       CoordConv.mni2labels(global_cursor_mni) ));
	if (DEBUG_TRACE) System.out.println("\t\t$CorVP._firePositionEvent DONE!\n");
    }

    final protected void _voxel2world( Point3Dfloat local_world, 
				       float local_vx, float local_vy, float local_vz) {
	Point3Dfloat global_world= new Point3Dfloat();
	if (!isNative) {
		if (DEBUG_VW) System.out.println("CorSlice2DVP -> CoordConv.voxel2world_common");
		CoordConv.voxel2world_common( global_world, local_vx, local_vz, local_vy);
	}
	else {
		if (DEBUG_VW) System.out.println("CorSlice2DVP -> CoordConv.voxel2world_nat");
		CoordConv.voxel2world_nat( global_world, local_vx, local_vz, local_vy);
	}
	// swap z & y :
	local_world.x= global_world.x;
	local_world.y= global_world.z;
	local_world.z= global_world.y;
    }

    final protected void _world2voxel( Point3Dint local_voxel,
				       float local_wx, float local_wy, float local_wz) {
	Point3Dint global_voxel= new Point3Dint();
	if (!isNative) {
		if (DEBUG_VW) System.out.println("symbol  : method _newCursCorSlice2DVP -> CoordConv.world2voxel_common");
		CoordConv.world2voxel_common( global_voxel, local_wx, local_wz, local_wy);
	}
	else {
		if (DEBUG_VW) System.out.println("CorSlice2DVP -> CoordConv.world2voxel_nat");
		CoordConv.world2voxel_nat( global_voxel, local_wx, local_wz, local_wy);
	}
	// swap z & y :
	local_voxel.x= global_voxel.x;
	local_voxel.y= global_voxel.z;
	local_voxel.z= global_voxel.y;
    }

    final protected void _world2voxel( Point3Dint local_voxel,
				       Point3Dfloat local_world) {
	Point3Dint global_voxel= new Point3Dint();
	if (!isNative) {
		if (DEBUG_VW) System.out.println("CorSlice2DVP -> CoordConv.world2voxel_common");
		CoordConv.world2voxel_common( global_voxel, local_world.x, local_world.z, local_world.y);
	}
	else {
		if (DEBUG_VW) System.out.println("CorSlice2DVP -> CoordConv.world2voxel_nat");
		CoordConv.world2voxel_nat( global_voxel, local_world.x, local_world.z, local_world.y);
	}
	// swap z & y :
	local_voxel.x= global_voxel.x;
	local_voxel.y= global_voxel.z;
	local_voxel.z= global_voxel.y;
    }
}
