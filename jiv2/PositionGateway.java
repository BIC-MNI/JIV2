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

import java.util.Vector;

/**
 * Provides a gateway ("firewall") for exchanging
 * <code>PositionEvent</code>-s between two sets of
 * <code>PositionListener/PositionGenerator</code>-s.
 * This way, the two sets (called "internal" and "external") are not
 * aware of each other.  The two interfaces implemented by this class
 * are for the "internal" side of the gateway.
 *
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: PositionGateway.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public final class PositionGateway extends PositionListenerAdapter 
    implements PositionGenerator {

    /** For development/testing only. Should be set to false in
        production code. */
    protected static final boolean DEBUG_OUTSIDE= false;
    protected static final boolean DEBUG= false;
    protected static final boolean DEBUG_TRACE= false;
    protected static final boolean DEBUG_HIGH= false;

    /*private*/ Vector internal_listeners= new Vector();
    /*private*/ Vector external_listeners= new Vector();

    /** This is used for INTERNALLY generated events to EXTERNAL (i.e. to a different panel) */
	// EXTERNAL doesn't count panels that are a combination of
    synchronized final public void positionChangeDetected( PositionEvent e) {
	if(DEBUG_TRACE) System.out.println("\t\t\t\t*PG.positionChangeDetected");
	if (external_listeners.size() > 0) {
		if (DEBUG_OUTSIDE) System.out.println("\n*****************************************\n");
		if (DEBUG) System.out.println("\nPositionGateway -> positionChangeDetected to be forwarded to another volume with e:"+e);
		if(DEBUG_TRACE) System.out.println("\t\t\t\t**PG.positionChangeDetected -> ~_forwardEvent()");
		_forwardEvent( external_listeners, e);
	}
	if(DEBUG_TRACE) System.out.println("\t\t\t\t*PG.positionChangeDetected DONE!\n");
    }
	
    /** This is used for INTERNAL listeners (i.e. within the same panel) */
    synchronized public void addPositionListener( PositionListener pl) {
	_addPositionListener( internal_listeners, pl);
    }

    /** This is used for INTERNAL listeners (i.e. within the same panel) */
    synchronized public void removePositionListener( PositionListener pl) {
	_removePositionListener( internal_listeners, pl);
    }

    /** This is used for EXTERNALLY generated events to INTERNAL (i.e. from different panel) */
    /** is only ever applied to DataVolumePanel class, not any of the other event listening classes */
    synchronized final public void positionChangeDetected_External( PositionEvent e) {
	if (internal_listeners.size() > 0) {
		if (DEBUG) System.out.println("\nPositionGateway -> positionChangeDetected from another volume with e:"+e);
		_forwardEvent( internal_listeners, e);
	}
    }
	
    synchronized public void addPositionListener_External( PositionListener pl) {
	_addPositionListener( external_listeners, pl);
    }

    synchronized public void removePositionListener_External( PositionListener pl) {
	_removePositionListener( external_listeners, pl);
    }

    // THIS could be passing an external event to internals, or an internal event to externals:
    final /*private*/ void _forwardEvent( Vector destinations, PositionEvent event)
    {
	if(DEBUG_TRACE) System.out.println("\t\t\t\t\t~_forwardEvent");

	if (DEBUG) System.out.println("\nPositionGateway -> _forwardEvent with e:"+event);
	for( int i= 0; i < destinations.size(); ++i){
	    PositionListener target = (PositionListener)destinations.elementAt( i);
	    if (DEBUG_HIGH) System.out.println("target:"+target);
	    if(DEBUG_TRACE) System.out.println("\t\t\t\t\t~~_forwardEvent -> *"+target+".positionChangeDetected()");
	    target.positionChangeDetected( event);
	}//end for

	if(DEBUG_TRACE) System.out.println("\t\t\t\t\t~_forwardEvent DONE!\n");
    }

    /*private*/ void _addPositionListener( Vector vec, PositionListener pl) {

	if( null == pl || vec.contains( pl))
	    return;
	vec.addElement( pl);
    }
    /*private*/ void _removePositionListener( Vector vec, PositionListener pl) {

	if( null != vec)
	    vec.removeElement( pl);
    }
}

