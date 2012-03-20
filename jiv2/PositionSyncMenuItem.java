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
 * A menu item that toggles the cursor position sync mode.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: PositionSyncMenuItem.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 *
 * @see Main#setPositionSync
 */
class PositionSyncMenuItem extends CheckboxMenuItem {

    /** used for debugging the IE4/5 checkboxmenuitem problem */
    /*protected*/ static final boolean		DEBUG_IE= false;
    /*private*/ Main 				main;

    PositionSyncMenuItem( Main applet_root) { 
	this( applet_root, (applet_root.getNumberOfPanels() > 1) ); 
    }
	
    PositionSyncMenuItem( Main applet_root, boolean enabled) {

	super( "Sync all cursors", applet_root.getPositionSync());
	main= applet_root;

	if( !enabled )
	    setEnabled( false);
	else
	    addItemListener( new ItemListener() {

		    public void itemStateChanged( ItemEvent e) {
			if( DEBUG_IE) System.out.println( this + ":" + e);

			switch( e.getStateChange()) {
			case ItemEvent.SELECTED:
			    if( DEBUG_IE) System.out.println( this + ": selected");
			    main.setPositionSync( true); 
			    break;
			case ItemEvent.DESELECTED:
			    if( DEBUG_IE) System.out.println( this + ": DEselected");
			    main.setPositionSync( false); 
			    break;
			}
		    }
		});
    }

} // end of class PositionSyncMenuItem

