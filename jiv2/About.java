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
import java.awt.event.*;

/**
 * Static class providing text descriptions (including copyright 
 * and license) for this software.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: About.java,v 2.0 2010/02/21 11:20:41 bailey $ 
 */
final class About {

    /** NB: this code assumes that the symbolic tags start with "ver" */
    /*private*/ static final String	raw_version= "$Name: ver_2_0 $";
    /*private*/ static final String 	program_name= "JIV2";
    /*private*/ static final String 	copyright= 
	"Copyright (C) 2000-2003 Chris A. Cocosco (crisco@bic.mni.mcgill.ca) and 2010 Lara Bailey (bailey@bic.mni.mcgill.ca)";
    // musing: maybe the following should be read from a 
    //         separate text file, (packed inside the JAR file) ?
    /*private*/ static final String[]	license= new String[] {
	program_name + " is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.",
	"",
	program_name + " is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.",
	"",
	"You should have received a copy of the GNU General Public License along with " + program_name + "; if not, see http://www.gnu.org/copyleft/gpl.html  , or write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA." 
	};
    /*private*/ static String 		formatted_version;

    static {
	int offset= raw_version.indexOf( "ver");
	formatted_version= (offset < 0 || offset > raw_version.length()-1) ?
	    "(unknown version)" : 
	    raw_version.substring( offset, raw_version.length()-1);
    }

    /**
     * @return a brief, one line description.
     */
    static final String getShortVersion()  {
	return program_name + " " + formatted_version + ",  " + copyright;
    }

    /**
     * @return pops a window with copyright info, and a 
     * summary of the license.
     */
    static final void popFullVersion( Frame parent ) {

	MultiLineStringBuffer text= new MultiLineStringBuffer();
	text.append_line( program_name + " " + formatted_version);
	text.append_line( copyright);
	text.append_line( "");
	for( int l= 0; l < license.length; ++l) 
	    text.append_line( license[ l]);

	InfoPopupWindow popup= new InfoPopupWindow( parent, "About...", text);
	return;
    }

} // end of class About
