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

import java.awt.Rectangle;

/**
 * An enhancement of the stock AWT <code>java.awt.Rectangle</code>.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: Rectangle2.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class Rectangle2 extends Rectangle {

    public final void expandToInclude( final Rectangle r) {

	final int x1= Math.min( x, r.x);
	final int x2= Math.max( x + width, r.x + r.width);
	final int y1= Math.min( y, r.y);
	final int y2= Math.max( y + height, r.y + r.height);

	this.x= x1;
	this.y= y1;
	this.width= x2 - x1;
	this.height= y2 - y1;
    }

} // end of class Rectangle2

