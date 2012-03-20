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

import java.awt.*;

/**
 * A stretchable vertical line (useful as a separator). It's
 * recommended to only stretch it in the vertical direction, because
 * the line will always be drawn at the x=0 horizontal position.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: VerticalLineComponent.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class VerticalLineComponent extends Panel {

    static final /*private*/ boolean 	DEBUG= false;
    static final /*private*/ Dimension 	MINIMUM_SIZE= new Dimension( 1, 1);
    
    final public Dimension getMinimumSize() { return MINIMUM_SIZE; }

    final public Dimension getPreferredSize() { return getMinimumSize(); }

    final public void paint( Graphics g) { update( g); }

    final public void update( Graphics g) { 
	if( DEBUG) 
	    System.out.println( this + " update(): getSize()=" + getSize());

	g.setColor( getForeground());
	g.drawLine( 0, 0, 0, getSize().height-1);
    }
}


