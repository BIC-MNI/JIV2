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

import java.util.*;

/**
 * An event type used for communicating position (cursor) changes, as
 * "world" coordinates. <br> <i>Beware:</i> for efficiency reasons,
 * the receivers (listeners) of <code>PositionEvents</code> are not
 * required to check the range of the new position received.  Thus, it
 * is sender's responsibility not to send positions outside the valid
 * range!
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: PositionEvent.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 *
 * @see PositionListener
 */
public final class PositionEvent extends EventObject {

    protected static final boolean      DEBUG= false;

    /*private*/ float position_x_mni;
    /*private*/ float position_y_mni;
    /*private*/ float position_z_mni;
    /*private*/ float position_x_nat;
    /*private*/ float position_y_nat;
    /*private*/ float position_z_nat;
    /*private*/ float position_post;
    /*private*/ float position_sup;
    /*private*/ float position_lat;

    /*private*/ static int source_type;
    public static final int MNI_EVENT= 0;
    public static final int LABEL_EVENT= 1;
    public static final int NAT_EVENT= 2;


    /*private*/ int changed_fields_mask;

    /** bit masks for the 'changed_fields_mask' argument (which should be
	the logical '|' of all the relevant bit masks */
    public static final int X_MNI= 1;
    public static final int Y_MNI= 2;
    public static final int Z_MNI= 4;
    public static final int X_NAT= 8;
    public static final int Y_NAT= 16;
    public static final int Z_NAT= 32;
    public static final int POST= 64;
    public static final int SUP= 128;
    public static final int LAT= 256;
    public static final int ALL_MNI= X_MNI | Y_MNI | Z_MNI;
    public static final int ALL_NAT= X_NAT | Y_NAT | Z_NAT;
    public static final int ALL_LABELS= POST | SUP | LAT;
    public static final int ALL= ALL_NAT | ALL_MNI | ALL_LABELS;
// X_MNI:1   Y_MNI:2    Z_MNI:4
// X_NAT:8   Y_NAT:16   Z_NAT:32
// POST:64    SUP:128    LAT:256
// XY_MNI:3  YZ_MNI:6   XZ_MNI:5
// XY_NAT:24 YZ_NAT:48  XZ_NAT:40
// ALL_MNI:7 ALL_NAT:56 ALL_LABELS:448??
// ALL:511??

    public PositionEvent( Object source, int source_type, int new_mask, 
			  Point3Dfloat position, Point3Dfloat position_nat, 
LabelCoords position_labels) {

	// simply invoke the other constructor...
	this( source, source_type, new_mask,
		 position.x, position.y, position.z,
		 position_nat.x, position_nat.y, position_nat.z,
		 position_labels.post, position_labels.sup, position_labels.lat);
    }

    public PositionEvent( Object source, int source_type, int new_mask, 
			  float new_x_mni, float new_y_mni, float new_z_mni,
			  float new_x_nat, float new_y_nat, float new_z_nat,
			  float new_post, float new_sup, float new_lat) {

	super( source);

	this.source_type= source_type;
	this.changed_fields_mask= new_mask;
	this.position_x_mni= new_x_mni;
	this.position_y_mni= new_y_mni;
	this.position_z_mni= new_z_mni;
	this.position_x_nat= new_x_nat;
	this.position_y_nat= new_y_nat;
	this.position_z_nat= new_z_nat;
	this.position_post= new_post;
	this.position_sup= new_sup;
	this.position_lat= new_lat;
	if (DEBUG) System.out.println("\nFiring new position event!\n"+this.toString());
    }


    final public int getFieldsMask() { return changed_fields_mask;}
    final public void setFieldsMask(int new_mask) { changed_fields_mask= new_mask;}
    final public int getSourceType() { return source_type;}
    final public boolean isMNISource() { return source_type == MNI_EVENT;}
    final public boolean isLabelSource() { return source_type == LABEL_EVENT;}
    final public boolean isNativeSource() { return source_type == NAT_EVENT;}
    final public boolean isXmniChanged() { return 0 != ( changed_fields_mask & X_MNI);}
    final public boolean isYmniChanged() { return 0 != ( changed_fields_mask & Y_MNI);}
    final public boolean isZmniChanged() { return 0 != ( changed_fields_mask & Z_MNI);}
    final public boolean isXnatChanged() { return 0 != ( changed_fields_mask & X_NAT);}
    final public boolean isYnatChanged() { return 0 != ( changed_fields_mask & Y_NAT);}
    final public boolean isZnatChanged() { return 0 != ( changed_fields_mask & Z_NAT);}
    final public boolean isPostChanged() { return 0 != ( changed_fields_mask & POST);}
    final public boolean isSupChanged() { return 0 != ( changed_fields_mask & SUP);}
    final public boolean isLatChanged() { return 0 != ( changed_fields_mask & LAT);}
    final public float getXmni() { return position_x_mni;}
    final public float getYmni() { return position_y_mni;}
    final public float getZmni() { return position_z_mni;}
    final public float getXnat() { return position_x_nat;}
    final public float getYnat() { return position_y_nat;}
    final public float getZnat() { return position_z_nat;}
    final public float getPost() { return position_post;}
    final public float getSup() { return position_sup;}
    final public float getLat() { return position_lat;}


    /** if possible, 
	these versions should be avoided (use the other getXYZ()) since
	it's hard on the memory manager: it creates a new Point3Dfloat
	object at each invocation... */
    final public Point3Dfloat getXYZmni() { 
	return new Point3Dfloat( position_x_mni, position_y_mni, position_z_mni);
    }
    final public Point3Dfloat getXYZnat() {
	return new Point3Dfloat( position_x_nat, position_y_nat, position_z_nat);
    }
    final public LabelCoords getPostSupLat() {
	return new LabelCoords( position_post, position_sup, position_lat);
    }

    final public void getXYZmni( Point3Dfloat result) { 
	result.x= position_x_mni;
	result.y= position_y_mni;
	result.z= position_z_mni;
    }
    final public void getXYZnat( Point3Dfloat result) { 
	result.x= position_x_nat;
	result.y= position_y_nat;
	result.z= position_z_nat;
    }
    final public void getPostSupLat( LabelCoords result) { 
	result.post= position_post;
	result.sup= position_sup;
	result.lat= position_lat;
    }

    final public String toString() {
	return "PositionEvent [mask=" + changed_fields_mask + 
	    ",pos=" + getXYZmni() + 
	    ",nat_pos=" + getXYZnat() +
	    ",label_pos=" + getPostSupLat() + "]" +
	    "\n\t\tsource=" + source;
    }
}

