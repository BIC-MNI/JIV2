
// $Id: Util.java,v 1.2 2001-05-16 23:12:31 crisco Exp $
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

import java.awt.*;

/**
 * A collection of various (<code>static</code>) utility functions.
 *
 * @author Chris Cocosco (crisco@bic.mni.mcgill.ca)
 * @version $Id: Util.java,v 1.2 2001-05-16 23:12:31 crisco Exp $
 */
public final class Util {

    /** used by reduceSignificantDigits() */
    /*private*/ static final int[] __chopFractionalPart_lookup= {
	1, 10, 100, 1000, 10000, 100000
    };

    /** Limitation: (original * 10^no_of_digits) has to fit in a
	signed integer (java integers are 32bit)
    */
    public static final float chopFractionalPart( float original,
						  int no_of_digits /** 0...5 */
						  ) {
	// use a lookup table for speed
	int mult= __chopFractionalPart_lookup[ no_of_digits];
	return Math.round( original * mult) / (float)mult;
    }

    /**
     * @return first enclosing <code>Frame</code> encountered up the
     * component hierarchy
     */
    public static final Frame getParentFrame( Component component) {
	
	Component f= component;
	while( f != null && !( f instanceof Frame ) ) 
	    f= f.getParent();
	return (Frame)f;
    }

    public static final String arrayToString( byte[] arg) {

	StringBuffer s= new StringBuffer( arg.toString() );
	s.append( " : ");
	for( int i= 0; i < arg.length; ++i) {
	    s.append( arg[ i]);
	    s.append( " ");
	}
	return s.toString();
    }

    public static final String arrayToString( int[] arg) {

	StringBuffer s= new StringBuffer( arg.toString() );
	s.append( " : ");
	for( int i= 0; i < arg.length; ++i) {
	    s.append( arg[ i]);
	    s.append( " ");
	}
	return s.toString();
    }

} // end of class Util
