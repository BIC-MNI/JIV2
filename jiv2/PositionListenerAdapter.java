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
 * An adapter (convenience) class for receiving
 * <code>PositionEvent</code>-s.  Listener classes that don't produce
 * slice data don't have to implement the
 * <code>getMaxSliceNumber</code> and <code>getOrthoStep</code> methods.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: PositionListenerAdapter.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public abstract class PositionListenerAdapter implements PositionListener {

    abstract public void positionChangeDetected( PositionEvent e );

    /** the implementing class has to return some meaningful (ie >0) value 
	only if it actually produces slice data... */
    public int getMaxSliceNumber() { return -1; }

    public float getOrthoStep() { return Float.NaN; }
}

