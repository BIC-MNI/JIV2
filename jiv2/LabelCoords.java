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

/**
 * A tuple of (post,sup,lat) float coordinates.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca) 
 * @version $Id: LabelCoords.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class LabelCoords {

    /** The posterior/anterior distance in the label volume */
    public float post;
    /** The superior/inferior distance in the label volume */
    public float sup;
    /** The medial/lateral distance in the label volume */
    public float lat;



    public LabelCoords() {}

    public LabelCoords( float post, float sup, float lat) {
	
	this.post= post;
	this.sup= sup;
	this.lat= lat;
    }

    public LabelCoords( LabelCoords src) {

	this( src.post, src.sup, src.lat);
    }

    public final void copyInto( LabelCoords dest) {
	dest.post= post;
	dest.sup= sup;
	dest.lat= lat;
    }

    public String toString() {

	return "post: "+ post + "(" + sup + "," + lat + ")";
    }
}
