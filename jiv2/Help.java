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

import java.applet.*;
import java.net.URL;

/**
 * Static class providing the help text functionality.
 * 
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: Help.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
final class Help {

    /** relative to CodeBase (or DocumentBase) */
    /*private*/ static final String 	HELP_URL= "doc/help/index.html";
    /*private*/ static final boolean 	DEBUG= false;

    static final void showHelp( Main applet_root) {

	try {
	    URL base= applet_root.getCodeBase();
	    if( null == base)
		base= applet_root.getDocumentBase();
	    /* NB: a non-null context value is expected by
	       this constructor (unless the 2nd arg is a
	       full url) */
	    URL url= new URL( base, HELP_URL);
	    /* show the help in a new browser window
	       Note: according to the Java docs, the
	       browser is allowed to ignore this
	       request... */
	    if( DEBUG) 
		System.out.println( "opening help url: " + url);

	    applet_root.getAppletContext().showDocument( url, "_blank");
	}
	catch( Exception ex) {
	    applet_root.progressMessage( "error! see console for details...");
	    System.err.println( "Error! " + ex);
	}
    }

} // end of class Help
