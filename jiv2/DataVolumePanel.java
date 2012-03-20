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
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

/**
 * Provides the functionality common for both volume panel types
 * (individual and combined).  <br><i>Note:</i> despite its name,
 * this is not a subclass of <code>awt.Panel</code>, and not even a
 * <code>Container</code>; its <code>Component</code>-s are managed by
 * an outside <code>Container</code>, which is supplied as an argument
 * to the constructor.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: DataVolumePanel.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
abstract public class DataVolumePanel 
//    extends PositionListenerAdapter implements PositionGenerator {
    extends PositionListenerAdapter {

    protected static final boolean	DEBUG= false;
    protected static final boolean	DEBUG_TRACE= false;
    protected static final boolean	DEBUG_POSITIONCHANGED= false;
    protected static final boolean	DEBUG_HIGH= false;
    protected static final boolean	DEBUG_VW= false;

    /** as per the spec of Double.toString() (in jdk1.1), this should
    be set to 11 in order to accomodate all posible outputs... but
    this would waste a lot of space in the gui... */
    protected static final int 		IMAGE_VALUE_TEXTFIELD_WIDTH= 6;

    /** The width of the anatomic label display textfield */
    protected static final int          ANAT_LABEL_TEXTFIELD_WIDTH=18;


    /** 
     * Support interface for the inner class
     * <code>DataVolumePanel.CoordinateFields</code>.
     *
     * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
     * @version $Id: DataVolumePanel.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
     *
     * @see DataVolumePanel.CoordinateFields */
    interface CoordinateTypes {

	int	VOXEL_COORDINATES= 0;
	int	WORLD_COORDINATES= 1;

	/* Note: on some platforms, '-' & '.' use up almost the same
           space as an '8' -- e.g. a world coords textfield width of 5
           is only _guaranteed_ to display 3 significant digits (for
           typical brain MNI-Talairach space world coordinates) ... */
	int[]	TEXTFIELD_WIDTH= { 
	    3, // voxel coords
	    5  // world coords
	};
    }

    /** The grid containing each of the data panels, title "JIV2" */
    /*private*/ Container 		parent_container;
    /** The window holding the above Container and the About.popFullVersion */
    /*private*/ Frame			parent_frame;
    /** our column in parent_container's grid */
    /*private*/ String 			post_field_label;
    /*private*/ String 			sup_field_label;
    /*private*/ String 			lat_field_label;
    /*private*/ int 			grid_column;
    protected boolean 			enable_world_coords;
    protected boolean 			byte_voxel_values;
    /*private*/  boolean 		isNative;
    /*private*/ VolumeHeader            local_sampling;

    /*private*/ Main			applet_root;
    /** initialized by the subclasses; 
	NB: they are expected to implement PositionListener too! */
    protected ImageProducer[]		slice_producers; // built-in java class
    /*private*/ Slice2DViewport[] 	slice_vports;
    /*private*/ Component		title;
    protected Container			controls_panel; 
    /*private*/ CoordinateFields	coord_fields;
    protected PopupMenu			popup_menu;
    /*private*/ PositionGateway		pos_event_gateway; 
    /*private*/ CheckboxMenuItem	sync_menu_item;
    /*private*/ Point3Dfloat		global_cursor_mni;
    /*private*/ Point3Dfloat		global_cursor_nat;


    public DataVolumePanel( /** it's expected to have a GridBagLayout-manager! */
				/** this is the jiv_frame created in Main.java */
			    Container parent_container,
			    /** to be used for GridBagConstraints.gridx */
			    String post_label, String sup_label, String lat_label,
			    int grid_column,
			    Point3Dfloat initial_cursor,
			    boolean enable_world_coords,
			    boolean byte_voxel_values,
			    boolean isNative,
			    VolumeHeader local_sampling,
			    Main applet_root
			    ) {

	if( !( parent_container.getLayout() instanceof GridBagLayout)) {
	    final String error_msg= 
		"supplied Container doesn't have a GridBagLayout layout manager";
	    throw new IllegalArgumentException( error_msg);
	}
	this.parent_container= parent_container;
	parent_frame= Util.getParentFrame( parent_container);
	if( null == parent_frame)
	    System.err.println( this + ": cannot determine my parent frame!");

	this.post_field_label= post_label;
	this.sup_field_label= sup_label;
	this.lat_field_label= lat_label;

	this.grid_column= grid_column;
	if(!isNative) {
		this.global_cursor_mni= initial_cursor;
		this.global_cursor_nat= CoordConv.mni2native(initial_cursor); }
	else {
		this.global_cursor_nat= initial_cursor;
		this.global_cursor_mni= CoordConv.native2mni(initial_cursor); }

	this.enable_world_coords= enable_world_coords;
	this.byte_voxel_values= byte_voxel_values;
	this.isNative= isNative;
	this.local_sampling= local_sampling;
	this.applet_root= applet_root;
	controls_panel= new LightweightPanel( new GridBagLayout());
	popup_menu= new PopupMenu();
	pos_event_gateway= new PositionGateway();
	if (DEBUG){
		if (isNative) System.out.println ("Creating native panel at col "+grid_column+".");
		else System.out.println ("Creating mni panel at col "+grid_column+".");
	}
    }

    /** completes the initalization process started by constructor(s) */
    protected final void _finish_initialization() {

	MouseAdapter popup_adapter= new MouseAdapter() {
	    // NB: need to override all 3 methods because on different 
	    // platforms the PopupTrigger could be delivered on different
	    // event types (e.g. mousePressed on Unix, mouseReleased on Win32)
	    public final void mousePressed( MouseEvent me) {
		if( me.isPopupTrigger()) _process_mouse_event( me);
	    }
	    public final void mouseReleased( MouseEvent me) {
		if( me.isPopupTrigger()) _process_mouse_event( me);
	    }
	    public final void mouseClicked( MouseEvent me) {
		if( me.isPopupTrigger()) _process_mouse_event( me);
	    }
	    /*private*/ final void _process_mouse_event( MouseEvent me) {
		if( DEBUG) {
		    System.out.println( me);
		    System.out.println( me.getSource() + " " + me.getComponent());
		}
		// in this context, 
		// 'e.getComponent()' is same as '(Component)me.getSource()'
		popup_menu.show( me.getComponent(), me.getX(), me.getY());
		me.consume();
	    }
	};
	/* NOTE: something poorly documented: a popup menu can only be
	   owned by one component at a time ! */
	parent_container.add( popup_menu);

	GridBagConstraints gbc= new GridBagConstraints();
	gbc.fill= GridBagConstraints.HORIZONTAL;
	gbc.weightx= 1.0;
	gbc.gridx= 0; 
	gbc.gridy= 0;
	// Note: the CoordinateFields constructor adds to popup_menu
	if (DEBUG) System.out.println("Constructing CoordFields..");
	controls_panel.add( coord_fields= 
			    new CoordinateFields( global_cursor_mni,
						  global_cursor_nat,
						  popup_menu,
						  enable_world_coords),
			    gbc);
	controls_panel.addMouseListener( popup_adapter);

	if (DEBUG) System.out.println("Constructing Viewports..");
	slice_vports= new Slice2DViewport[] { 
	    new TransverseSlice2DViewport( slice_producers[ 0], local_sampling,
					   getTitle(),
					   (PositionListener)slice_producers[ 0],
					   global_cursor_mni, 
					   global_cursor_nat, 
					   isNative),
	    new SagittalSlice2DViewport( slice_producers[ 1], local_sampling,
					 getTitle(),
					 (PositionListener)slice_producers[ 1],
					   global_cursor_mni, 
					   global_cursor_nat, 
					   isNative),
	    new CoronalSlice2DViewport( slice_producers[ 2], local_sampling,
					getTitle(),
					(PositionListener)slice_producers[ 2],
					   global_cursor_mni, 
					   global_cursor_nat, 
					   isNative),
	};


	for( int i= 0; i < 3; ++i) 
	    slice_vports[ i].addMouseListener( popup_adapter);

	gbc.gridx= grid_column;
	gbc.gridy= GridBagConstraints.RELATIVE;
	gbc.weightx= 1.0;
	gbc.insets.bottom= gbc.insets.top= 0;
	gbc.insets.left= gbc.insets.right= 5;
	gbc.fill= GridBagConstraints.HORIZONTAL;
	gbc.weighty= 0;
	parent_container.add( title= new Label( getTitle(), Label.CENTER), gbc);
	gbc.insets.bottom= gbc.insets.top= 3;
	gbc.fill= GridBagConstraints.BOTH;
	for( int i= 0; i < 3; ++i) {
	    // transverse, sagittal, coronal
	    gbc.weighty= slice_vports[ i].getOriginalImageHeight();
	    parent_container.add( slice_vports[ i], gbc); 
	}
	gbc.fill= GridBagConstraints.HORIZONTAL;
	gbc.weighty= 0;
	gbc.anchor= GridBagConstraints.NORTH;
	parent_container.add( controls_panel, gbc);

	// hookup the position communication lines
	Vector listeners= new Vector();
	Vector generators= new Vector();
	for( int i= 0; i < 3; ++i) {
	    listeners.addElement( slice_producers[ i]);
	    listeners.addElement( slice_vports[ i]);
	    generators.addElement( slice_vports[ i]);
	}
	listeners.addElement( coord_fields);
	listeners.addElement( pos_event_gateway);
	generators.addElement( coord_fields);
	generators.addElement( pos_event_gateway);

	for( int g= 0; g < generators.size(); ++g) { 
	    Object peg= generators.elementAt( g);
	    if (DEBUG)
		System.out.println("\ngen: " + peg + "\ncan be heard by:");
	    for( int l= 0; l < listeners.size(); ++l) {
		Object pl= listeners.elementAt( l);
		if( peg != pl) {
		    // don't want to listen to my own babble
		    if( DEBUG) {
			String class_name=pl.getClass().getName();
//			if (class_name.contains(".DataVolumePanel")){
				System.out.println( "listener: " + pl);
//			}// end if class_name
		    }// end if DEBUG
		    // this method is smart enough not to add duplicates
		    ((PositionGenerator)peg).addPositionListener( (PositionListener)pl);
		}
	    }
	}
	// any general (shared) popup menu commands should be added here:
	sync_menu_item= new PositionSyncMenuItem( applet_root);
	popup_menu.add( sync_menu_item);
	popup_menu.addSeparator();
	Menu hm= new Menu( "Help", true /* a "tear-off" menu */ );
	{
	    MenuItem help= new MenuItem( "Help");
	    help.addActionListener( new ActionListener() {
		    public void actionPerformed( ActionEvent ev) {
			Help.showHelp( applet_root);
		    }
		});
	    hm.add( help); 
	    MenuItem about= new MenuItem( "About JIV2");
	    about.addActionListener( new ActionListener() {
		    public void actionPerformed( ActionEvent e) {
			if( parent_frame != null)
			    About.popFullVersion( parent_frame);
			else
			    applet_root.progressMessage( About.getShortVersion());
		    }
		});
	    hm.add( about);
	}
	popup_menu.add( hm);
	popup_menu.add( new QuitMenuItem( applet_root));
	// TODO(maybe): add a kbd shortcut as well?

    } // end of _finish_initialization()

    //This is only used when sync mode is on to recieve events from other panels.
    public final void positionChangeDetected( PositionEvent e) {
	if (DEBUG_TRACE) System.out.println("\t\t\t\t*DVP.positionChangeDetected");
	if (DEBUG_TRACE) System.out.println("\t\t\t\t**DVP.positionChangeDetected -> pos_event_gateway.positionChangeDetectedExternal()");
	pos_event_gateway.positionChangeDetected_External( e);
	if (DEBUG_TRACE) System.out.println("\t\t\t\t*DVP.positionChangeDetected DONE!\n");
    }

    // This is never used!!
    // It was just here so the class could implement PositionGenerator,
    // which is not necessary!
//    public void addPositionListener( PositionListener pl) {
//	addPositionListener( pl, false);
//    }

    // This is for inter_panel listening, and is called by: Main.java (setPositionSync)
    public void addPositionListener( PositionListener pl, boolean send_event) {
	pos_event_gateway.addPositionListener_External( pl);
	if( send_event) {
	    PositionEvent event= 
		/* Note: the PositionEvent constructor makes a _copy_ of
		   its last argument (the cursor) */
		new PositionEvent( this, 
				   isNative ? PositionEvent.NAT_EVENT : PositionEvent.MNI_EVENT,
				   PositionEvent.ALL,
				   coord_fields.getMNICursorPosition(),
				   coord_fields.getNativeCursorPosition(),
				   coord_fields.getLabelCoords() );
	    pl.positionChangeDetected( event);
	}
    }


    public void removePositionListener( PositionListener pl) {
	pos_event_gateway.removePositionListener_External( pl);
    }

    /** it should be 'synchronized', but not necessary because Main is the 
	only one calling it...
    */
    public void setPositionSync( boolean new_setting) {
	sync_menu_item.setState( new_setting);
    }

    /** returns a short instead of the more logical byte merely for convenience
	(bytes are signed, so additional code is needed to interpret bytes as
	unsigned, i.e. 0...255) 
    */
    protected final short _string2voxel( String string) 
	throws NumberFormatException {

	if( byte_voxel_values)
	    return Short.parseShort( string);

	return _image_real2byte( Float.valueOf( string).floatValue());
    }

    protected final String _voxel2string( int voxel_value) {

	if( byte_voxel_values)
	    return String.valueOf( voxel_value);

	float real_value= _image_byte2real( (short)voxel_value);
	return String.valueOf( Util.chopToNSignificantDigits( real_value, 3));
    }

 //Only used to check textfield coord requests
    abstract public int getXmniSize();
    abstract public int getYmniSize();
    abstract public int getZmniSize();
    abstract public int getXnatSize();
    abstract public int getYnatSize();
    abstract public int getZnatSize();
    abstract public int getPostMax();
    abstract public int getSupMax();
    abstract public int getLatMax();
    abstract public int getPostMin();
    abstract public int getSupMin();
    abstract public int getLatMin();

    abstract public String getTitle();

    /** This is an aid to CoordinateFields:
	if the actual implementation returns (always) a negative value, then
	the "value field" won't be displayed
    */
    abstract protected int _getVoxelValue( Point3Dint voxel_pos);

    abstract protected String _getLabelValue( Point3Dfloat world_mni);

    abstract protected float _image_byte2real( short voxel_value);

    abstract protected short _image_real2byte( float image_value);


    /** 
     * Member (inner) class: the textual coordinate display/input
     * boxes ("fields").
     *
     * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
     * @version $Id: DataVolumePanel.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
     *
     * @see DataVolumePanel.CoordinateTypes 
     */
    /*private*/ final class CoordinateFields extends LightweightPanel 
	implements CoordinateTypes, ActionListener, PositionListener, PositionGenerator {

	// FIXME: it doesn't handle well an allocated width which is 
	// too small to fit everything (horizontally) - left/right
	// ends are not visible/accesible...

	/* Note: these two should be kept in sync (we use a separate
           int field simply for execution speed (its value is tested
           inside changePosition(), which is typically called very
           often... */
	/*private*/ int			coordinates_type= WORLD_COORDINATES;
	/*private*/ ChoiceMenu		coords_type_menu;

	/*private*/ TextField 		x_field_mni;
	/*private*/ TextField 		y_field_mni;
	/*private*/ TextField 		z_field_mni;

        /*private*/ TextField           x_field_native;
        /*private*/ TextField           y_field_native;
        /*private*/ TextField           z_field_native;

        /*private*/ TextField           post_field;
        /*private*/ TextField           lat_field;
        /*private*/ TextField           sup_field;

        /*private*/ TextField           anatomy_label_field;
	/*private*/ TextField 		intensity_value_field;

	/*private*/ Vector 		event_listeners;
	/* Beware! these six should always be in sync! */
	/*private*/ Point3Dfloat 	world_cursor_mni;	/** world coordinates in mni space */
	/*private*/ Point3Dint 		voxel_cursor_mni;	/** voxel coordinates in mni space */
	/*private*/ Point3Dfloat 	world_cursor_nat;	/** world coordinates in native space */
	/*private*/ Point3Dint 		voxel_cursor_nat;	/** voxel coordinates in native space */
	/*private*/ LabelCoords		label_coords;		/** label coordinates in mni space */
	/*private*/ String 		anatomy_label;		/** anatomy label in panel space */

	protected CoordinateFields( Point3Dfloat world_cursor_mni,
				    Point3Dfloat world_cursor_nat,
				    PopupMenu popup_menu,
				    boolean enable_world_coords
				    ) {

	    this.world_cursor_mni= world_cursor_mni;
	    this.world_cursor_nat= world_cursor_nat;
	    // initialization for voxel_cursor_mni
	    this.voxel_cursor_mni= _world2voxel_mni( world_cursor_mni);
	    this.voxel_cursor_nat= _world2voxel_nat( world_cursor_nat);
	    this.label_coords= CoordConv.mni2labels( world_cursor_mni);
	    this.anatomy_label= _getLabelValue( world_cursor_mni);

	    /** label-value(-flag) tuples to be used in the "coords type" menu */
	    final Object[][] menu_definition= {
		{ "world", new Integer( WORLD_COORDINATES), 
		  new Boolean( !enable_world_coords) 
		},
		{ "voxel", new Integer( VOXEL_COORDINATES) 
		}
	    };
	    coordinates_type= 
		enable_world_coords ? WORLD_COORDINATES : VOXEL_COORDINATES;
	    /* determine the string label of the menu entry
               corresponding to 'coordinates_type' */
	    int i;
	    for( i= 0; i < menu_definition.length; ++i) 
		if( coordinates_type == ((Integer)menu_definition[ i][ 1]).intValue() )
		    break;
	    // if we didn't find it, then i == menu_definition.length and the JVM
	    // will throw an ArrayIndexOutOfBoundsException on the following line:
	    String default_menu_selection= (String)menu_definition[ i][ 0];
	    ActionListener al= new ActionListener() {
		public void actionPerformed( ActionEvent ae) {
		    CoordinateFields.this.changeCoordinatesType();
		}
	    };
	    coords_type_menu= new ChoiceMenu( "Coordinates type",
					      menu_definition, default_menu_selection, 
					      al); 
	    popup_menu.add( coords_type_menu);

	    setLayout( new GridBagLayout());
	    GridBagConstraints gbc= new GridBagConstraints();
	    gbc.gridx= GridBagConstraints.RELATIVE;
	    gbc.gridy= 0;
	    gbc.fill= GridBagConstraints.NONE;
	    final int tf_width= TEXTFIELD_WIDTH[ coordinates_type];
	    final int tf_span= 1;
	    x_field_mni= _add_field_with_label( "X", "", tf_width, tf_span, gbc);
	    y_field_mni= _add_field_with_label( "Y", "", tf_width, tf_span, gbc);
	    z_field_mni= _add_field_with_label( "Z", "", tf_width, tf_span, gbc);
	    gbc.gridy= 1;
	    x_field_native= _add_field_with_label( "Xnat", "", tf_width, tf_span, gbc);
	    y_field_native= _add_field_with_label( "Ynat", "", tf_width, tf_span, gbc);
	    z_field_native= _add_field_with_label( "Znat", "", tf_width, tf_span, gbc);
	    gbc.gridy = 2;
	    post_field= _add_field_with_label( post_field_label, "", tf_width, tf_span, gbc);
	    lat_field= _add_field_with_label( lat_field_label, "", tf_width, tf_span, gbc);
	    sup_field= _add_field_with_label( sup_field_label, "", tf_width, tf_span, gbc);
	    gbc.gridy = 3;

	    // The non-editable anatomy label display field:
	    anatomy_label_field= _add_field_with_label( "Anatomy",
						anatomy_label,
						ANAT_LABEL_TEXTFIELD_WIDTH,
						3,
						gbc);
	    anatomy_label_field.setEditable( false);


	    /* send myself an event, such that positionChangeDetected() will
               fill-in the correct type of coordinates... */
	    positionChangeDetected( new PositionEvent( CoordinateFields.this,
					isNative ? PositionEvent.NAT_EVENT : PositionEvent.MNI_EVENT,
					PositionEvent.ALL,
					world_cursor_mni,
					world_cursor_nat,
					label_coords ) );

	    // The non-editable voxel intensity label display field:
	    int voxel_value;
	    Point3Dint voxel_cursor_common;
	    if (!isNative)
	    	voxel_value= _getVoxelValue( _world2voxel_common( world_cursor_mni));
	    else
		voxel_value= _getVoxelValue( _world2voxel_nat( world_cursor_nat));


	    // If this is a CombinedDataVolumePanel, don't display intensity
	    if( voxel_value >= 0) {
		intensity_value_field= _add_field_with_label( "I",
						_voxel2string( voxel_value), 
						//byte_voxel_values ? 3 : 
						//IMAGE_VALUE_TEXTFIELD_WIDTH,
						tf_width,
						tf_span,
						gbc);
		intensity_value_field.setEditable( false);
	    }
	    else {    // probably unnecessary...
		intensity_value_field= null;
	    }

	    // Activate textfields as generators so this class listens to them
	    this.addPositionListener( this); 
	    x_field_mni.addActionListener( this);
	    y_field_mni.addActionListener( this);
	    z_field_mni.addActionListener( this);
	    x_field_native.addActionListener( this);
	    y_field_native.addActionListener( this);
	    z_field_native.addActionListener( this);
	    post_field.addActionListener( this);
	    lat_field.addActionListener( this);
	    sup_field.addActionListener( this);
	} // end $CoordinateFields Constructor
	
	/** convenience method */
	/*private*/ TextField _add_field_with_label( String text_label,
						     String initial_content,
						     int width,
						     int span_columns,
						     GridBagConstraints gbc) {
	    gbc.insets.left= 0;
	    gbc.gridwidth = 1;
	    gbc.weightx= 1.0;
	    gbc.anchor= GridBagConstraints.EAST;
	    add( new Label( text_label + ":"), gbc);

	    gbc.weightx= 0;
	    gbc.anchor= GridBagConstraints.WEST;
	    gbc.insets.left= 0;
	    gbc.insets.right= 0;
	    gbc.gridwidth = span_columns;
	    TextField text_field= new TextField( initial_content, width);
	    add( text_field, gbc);

	    gbc.insets.left= 0;

	    return text_field;
	}


	// This is only performed when textfield position events are changed - not when mouse cursor is moved!
	// This is called by "actionPerformed( ActionEvent ae)"
	final /*private*/ void _firePositionEvent( final PositionEvent e) {

	    if (DEBUG_POSITIONCHANGED) System.out.println( "DataVolumePanel$CoordFields -> _firePositionEvent with");
	    if (DEBUG_POSITIONCHANGED) System.out.println("\t"+e);

	    //Since any change in a native coord means all the mni coords change
	    //and any change in an mni coord means all the native coords change
	    //This is a good place to adjust the mask so they will detect the changes.
	    int old_mask= e.getFieldsMask();
	    int new_mask;
	    if (e.isMNISource())  
		new_mask= old_mask | PositionEvent.ALL_NAT | PositionEvent.ALL_LABELS;
	    else if (e.isLabelSource())
		new_mask= old_mask | PositionEvent.ALL_MNI | PositionEvent.ALL_NAT;
	    else
		new_mask= old_mask | PositionEvent.ALL_MNI | PositionEvent.ALL_LABELS;
	    e.setFieldsMask(new_mask);

	    // deliver the event to each of the listeners
	    if( null == event_listeners) 
		// nobody listening...
		return;
	    for( int i= 0; i < event_listeners.size(); ++i)
		((PositionListener)event_listeners.elementAt( i)).positionChangeDetected( e);

	}// end _firePositionEvent(e)

	synchronized /*private*/ final void changeCoordinatesType() { 

	    coordinates_type= ((Integer)coords_type_menu.getSelection()).intValue();

	    int new_width= TEXTFIELD_WIDTH[ coordinates_type];
	    x_field_mni.setColumns( new_width);
	    y_field_mni.setColumns( new_width);
	    z_field_mni.setColumns( new_width);

	    x_field_native.setColumns( new_width);
	    y_field_native.setColumns( new_width);
	    z_field_native.setColumns( new_width);

	    post_field.setColumns( new_width);
	    lat_field.setColumns( new_width);
	    sup_field.setColumns( new_width);

	    // FIXME: these setColumns() currently don't have any effect... ??

	    /* send myself an event, such that positionChangeDetected() will
	       update the coordinate fields with the new format... */
	    PositionEvent pe= 
		new PositionEvent( CoordinateFields.this,
				   isNative ? PositionEvent.NAT_EVENT : PositionEvent.MNI_EVENT,
				   PositionEvent.ALL,
				   world_cursor_mni,
				   world_cursor_nat,
				   label_coords );
	    positionChangeDetected( pe);
	}

	/** for the exclusive private/internal use of actionPerformed */
	/*private*/ Point3Dint __actionPerformed_voxel= new Point3Dint();


	/** 
	* This is only performed when a textfield is altered, not when mouse is clicked..
	* It detects an alteration to a textfield, updates the relevant cursor, syncs the rest,
	* then fires a PositionEvent to let others know
	*/
	synchronized public final void actionPerformed( ActionEvent ae) {

	    if (DEBUG_POSITIONCHANGED) System.out.println( "\nDataVolumePanel$CoordFields -> actionPerformed");
	    if (DEBUG_HIGH) System.out.println("\twith ae: "+ae);

	    if( ae.getID() != ActionEvent.ACTION_PERFORMED)
		return;

	    int pos_event_mask;
	    TextField source= (TextField)ae.getSource();
	    // alias them to local (stack) var-s for speed:
	    final Point3Dfloat 	world_cursor_mni= this.world_cursor_mni;
	    final Point3Dint 	voxel_cursor_mni= this.voxel_cursor_mni;
	    final Point3Dfloat 	world_cursor_nat= this.world_cursor_nat;
	    final Point3Dint 	voxel_cursor_nat= this.voxel_cursor_nat;
	    final LabelCoords 	label_coords= this.label_coords;

	    // similar to isNative, but reflects which textfield group is modified instead of volume type
	    int source_type;
	    int max_dimension_size;
	    int min_dimension_size;

	    if( VOXEL_COORDINATES == coordinates_type) {
		/* == VOXEL COORDINATES == */
		try {
		    //Get new value (as both, because if it's an anatomy label textfield, it's a float)
		    int new_value_int= Short.parseShort( ae.getActionCommand()); 
		    float new_value_float= Float.valueOf( ae.getActionCommand()).floatValue();

		    //Get old_value, max_dimension_size, min_dimension_size
		    //They are needed to check validity of new_value
		    int old_value_int;
		    float old_value_float; // because label_coords are floats
		    if( source == x_field_mni) {
			pos_event_mask= PositionEvent.X_MNI;
			source_type= PositionEvent.MNI_EVENT;
			max_dimension_size= getXmniSize();
			min_dimension_size= 0;
			old_value_int= voxel_cursor_mni.x;
			old_value_float= 0;
		    }
		    else if( source == y_field_mni) {
			pos_event_mask= PositionEvent.Y_MNI;
			source_type= PositionEvent.MNI_EVENT;
			max_dimension_size= getYmniSize();
			min_dimension_size= 0;
			old_value_int= voxel_cursor_mni.y;
			old_value_float= 0;
		    }
		    else if( source == z_field_mni) {
			pos_event_mask= PositionEvent.Z_MNI;
			source_type= PositionEvent.MNI_EVENT;
			max_dimension_size= getZmniSize();
			min_dimension_size= 0;
			old_value_int= voxel_cursor_mni.z;
			old_value_float= 0;
		    }
		    else if( source == x_field_native) {
			pos_event_mask= PositionEvent.X_NAT;
			source_type= PositionEvent.NAT_EVENT;
//### THIS IS NOT CORRECT!
			max_dimension_size= getXnatSize();
			min_dimension_size= 0;
			old_value_int= voxel_cursor_nat.x;
			old_value_float= 0;
		    }
		    else if( source == y_field_native) {
			pos_event_mask= PositionEvent.Y_NAT;
			source_type= PositionEvent.NAT_EVENT;
			max_dimension_size= getYnatSize();
			min_dimension_size= 0;
			old_value_int= voxel_cursor_nat.y;
			old_value_float= 0;
		    }
		    else if( source == z_field_native) {
			pos_event_mask= PositionEvent.Z_NAT;
			source_type= PositionEvent.NAT_EVENT;
			max_dimension_size= getZnatSize();
			min_dimension_size= 0;
			old_value_int= voxel_cursor_nat.z;
			old_value_float= 0;
		    }
		    else if( source == post_field) {
			pos_event_mask= PositionEvent.POST;
			source_type= PositionEvent.LABEL_EVENT;
			max_dimension_size= getPostMax();
			min_dimension_size= getPostMin();
			old_value_float= label_coords.post;
			old_value_int= 0;
		    }
		    else if( source == sup_field) {
			pos_event_mask= PositionEvent.SUP;
			source_type= PositionEvent.LABEL_EVENT;
			max_dimension_size= getSupMax();
			min_dimension_size= getSupMin();
			old_value_float= label_coords.sup;
			old_value_int= 0;
		    }
		    else if( source == lat_field) {
			pos_event_mask= PositionEvent.LAT;
			source_type= PositionEvent.LABEL_EVENT;
			max_dimension_size= getLatMax();
			min_dimension_size= getLatMin();
			old_value_float= label_coords.lat;
			old_value_int= 0;
		    }
		    else
			throw new IllegalArgumentException( "unexpected source:" + ae);

		    //If number is the same, do nothing
		    if (source_type != PositionEvent.LABEL_EVENT) {
			if (new_value_int == old_value_int) {
			    if(DEBUG_POSITIONCHANGED)
				System.out.println("Actually no change from original: "+old_value_int+"\n\n");
			    return;
			}
		    }
		    else {
			if (new_value_float == old_value_float) {
			    if(DEBUG_POSITIONCHANGED)
				System.out.println("Actually no change from original: "+old_value_float+"\n\n");
			    return;
			}
		    }

		    // remember: this, the event producer should check the range!
		    if( new_value_int <= min_dimension_size || new_value_int >= max_dimension_size)
				throw new NumberFormatException( "out of range...");


		    // Now, update cursors with new_value
		    if( source == x_field_mni)
			voxel_cursor_mni.x = new_value_int;
		    else if( source == y_field_mni)
			voxel_cursor_mni.y = new_value_int;
		    else if( source == z_field_mni)
			voxel_cursor_mni.z = new_value_int;
		    else if( source == x_field_native)
			voxel_cursor_nat.x = new_value_int;
		    else if( source == y_field_native)
			voxel_cursor_nat.y = new_value_int;
		    else if( source == z_field_native)
			voxel_cursor_nat.z = new_value_int;
		    else if( source == post_field)
			label_coords.post = new_value_float;
		    else if( source == sup_field)
			label_coords.sup = new_value_float;
		    else
			label_coords.lat = new_value_float;

		    //Now resync cursors
		    if (source_type == PositionEvent.MNI_EVENT) {
			CoordConv.voxel2world_mni( world_cursor_mni, voxel_cursor_mni);
		        CoordConv.mni2native(world_cursor_nat, world_cursor_mni);
			CoordConv.mni2labels(label_coords, world_cursor_mni);
		    }
		    else if (source_type == PositionEvent.NAT_EVENT) {
			CoordConv.voxel2world_nat( world_cursor_nat, voxel_cursor_nat);
			CoordConv.native2mni( world_cursor_mni, world_cursor_nat);
			CoordConv.mni2labels(label_coords, world_cursor_mni);
		    }
		    else if (source_type == PositionEvent.LABEL_EVENT) {
			CoordConv.labels2mni( world_cursor_mni, label_coords);
		        CoordConv.mni2native(world_cursor_nat, world_cursor_mni);
		    }

		    /* Note: the PositionEvent constructor makes a _copy_
		       of the cursors */
		    _firePositionEvent( new PositionEvent( CoordinateFields.this, 
							   source_type, 
							   pos_event_mask, 
							   world_cursor_mni,
							   world_cursor_nat,
							   label_coords ));
		}// end try
		catch( NumberFormatException exception) { 
		    /* the code below is suboptimal (we could figure out
		       which one text field to update), but it's only run
		       in exceptional situations (and when speed is not an
		       issue because the user just typed something...) */
		    x_field_mni.setText( String.valueOf( voxel_cursor_mni.x));
		    y_field_mni.setText( String.valueOf( voxel_cursor_mni.y));
		    z_field_mni.setText( String.valueOf( voxel_cursor_mni.z));
		    x_field_native.setText( String.valueOf( voxel_cursor_nat.x));
		    y_field_native.setText( String.valueOf( voxel_cursor_nat.y));
		    z_field_native.setText( String.valueOf( voxel_cursor_nat.z));
		    post_field.setText( myRound( label_coords.post));
		    lat_field.setText( myRound( label_coords.lat));
		    sup_field.setText( myRound( label_coords.sup));
		}
		return;
	    }
	    /* == WORLD COORDINATES == */
	    try {
		// new_value could be int if it originated from anatomy label coord textfields
		// so, store both new_value_float and new_value_int:
		float new_value_world= Float.valueOf( ae.getActionCommand()).floatValue();

		//Get old_value, max_dimension_size, min_dimension_size
		//They are needed to check validity of new_value
		//Also set source_type (to allow testing on int if source is anatomy label field)
		//Also set new_value_vox (it depends on which field new_value originated in)
		float old_value_world;
		int new_value_vox;
		if( source == x_field_mni) {
			pos_event_mask= PositionEvent.X_MNI;
			source_type= PositionEvent.MNI_EVENT;
			max_dimension_size= getXmniSize();
			min_dimension_size= 0;
			_world2voxel_mni( __actionPerformed_voxel, 
				new_value_world, world_cursor_mni.y, world_cursor_mni.z);
			new_value_vox= __actionPerformed_voxel.x;
			old_value_world= world_cursor_mni.x;
		}
		else if( source == y_field_mni) {
			pos_event_mask= PositionEvent.Y_MNI;
			source_type= PositionEvent.MNI_EVENT;
			max_dimension_size= getYmniSize();
			min_dimension_size= 0;
			_world2voxel_mni( __actionPerformed_voxel, 
				world_cursor_mni.x, new_value_world, world_cursor_mni.z);
			new_value_vox= __actionPerformed_voxel.y;
			old_value_world= world_cursor_mni.y;
		}
		else if( source == z_field_mni) {
			pos_event_mask= PositionEvent.Z_MNI;
			source_type= PositionEvent.MNI_EVENT;
			max_dimension_size= getZmniSize();
			min_dimension_size= 0;
			_world2voxel_mni( __actionPerformed_voxel, 
				world_cursor_mni.x, world_cursor_mni.y, new_value_world);
			new_value_vox= __actionPerformed_voxel.z;
			old_value_world= world_cursor_mni.z;
		}
		else if( source == x_field_native) {
			pos_event_mask= PositionEvent.X_NAT;
			source_type= PositionEvent.NAT_EVENT;
			max_dimension_size= getXnatSize();
			min_dimension_size= 0;
			_world2voxel_nat( __actionPerformed_voxel, 
				new_value_world, world_cursor_nat.y, world_cursor_nat.z);
			new_value_vox= __actionPerformed_voxel.x;
			old_value_world= world_cursor_nat.x;
		}
		else if( source == y_field_native) {
			pos_event_mask= PositionEvent.Y_NAT;
			source_type= PositionEvent.NAT_EVENT;
			max_dimension_size= getYnatSize();
			min_dimension_size= 0;
			_world2voxel_nat( __actionPerformed_voxel, 
				world_cursor_nat.x, new_value_world, world_cursor_nat.z);
			new_value_vox= __actionPerformed_voxel.y;
			old_value_world= world_cursor_nat.y;
		}
		else if( source == z_field_native) {
			pos_event_mask= PositionEvent.Z_NAT;
			source_type= PositionEvent.NAT_EVENT;
			max_dimension_size= getZnatSize();
			min_dimension_size= 0;
			_world2voxel_nat( __actionPerformed_voxel, 
				world_cursor_nat.x, world_cursor_nat.y, new_value_world);
			new_value_vox= __actionPerformed_voxel.z;
			old_value_world= world_cursor_nat.z;
		}
		else if( source == post_field) {
			pos_event_mask= PositionEvent.POST;
			source_type= PositionEvent.LABEL_EVENT;
			max_dimension_size= getPostMax();
			min_dimension_size= getPostMin();
			new_value_vox= (int) new_value_world;
			old_value_world= label_coords.post;
		}
		else if( source == sup_field) {
			pos_event_mask= PositionEvent.SUP;
			source_type= PositionEvent.LABEL_EVENT;
			max_dimension_size= getSupMax();
			min_dimension_size= getSupMin();
			new_value_vox= (int) new_value_world;
			old_value_world= label_coords.sup;
		}
		else if( source == lat_field) {
			pos_event_mask= PositionEvent.LAT;
			source_type= PositionEvent.LABEL_EVENT;
			max_dimension_size= getLatMax();
			min_dimension_size= getLatMin();
			new_value_vox= (int) new_value_world;
			old_value_world= label_coords.lat;
		}
		else 
		    throw new IllegalArgumentException( "unexpected source:" + ae);

		//If number is the same, do nothing
		if (new_value_world == old_value_world) {
			if(DEBUG_POSITIONCHANGED) System.out.println("Actually no change from original: "+old_value_world+"\n\n");
			return;
		}

		// remember: this, the event producer should check the range!
		// (need to check in voxel not world coords)
		/* FIXME: the range test below _assumes_ that the
			world coords axes are the same as the voxel
			coords axes! */
		if( new_value_vox <= min_dimension_size || new_value_vox >= max_dimension_size)
			throw new NumberFormatException( "out of range...");

		// Now, update cursors with new_value_world:
		// recall: voxel_cursor_mni and world_cursor_mni
		// are always consistent with each other!
		// and: world_cursor_mni and world_cursor_nat
		// should always be consistent as well..
		if( source == x_field_mni) {
		    world_cursor_mni.x= new_value_world;
		    voxel_cursor_mni.x= new_value_vox;
		}
		else if( source == y_field_mni) 
		    world_cursor_mni.y= new_value_world;
		else if( source == z_field_mni)
		    world_cursor_mni.z= new_value_world;
		else if( source == x_field_native)
		    world_cursor_nat.x= new_value_world;
		else if( source == y_field_native)
		    world_cursor_nat.y= new_value_world;
		else if( source == z_field_native)
		    world_cursor_nat.z= new_value_world;
		else if( source == post_field)
		    label_coords.post= new_value_world;
		else if( source == sup_field)
		    label_coords.sup= new_value_world;
		else //( source == lat_field)
		    label_coords.lat= new_value_world;

		//Now, resync cursors:
		if (source_type == PositionEvent.MNI_EVENT) {
		    CoordConv.mni2native(world_cursor_nat, world_cursor_mni);
		    CoordConv.mni2labels(label_coords, world_cursor_mni);
		}
		else if (source_type == PositionEvent.NAT_EVENT) {
		    CoordConv.native2mni(world_cursor_mni, world_cursor_nat);
		    CoordConv.mni2labels(label_coords, world_cursor_mni);
		}
		else if (source_type == PositionEvent.LABEL_EVENT) {
		    CoordConv.labels2mni( world_cursor_mni, label_coords);
		    CoordConv.mni2native(world_cursor_nat, world_cursor_mni);
		}


		/* Note: the PositionEvent constructor makes a _copy_
		   of its last two arguments (the cursors) */
		_firePositionEvent( new PositionEvent( CoordinateFields.this, 
						       source_type, 
						       pos_event_mask, 
						       world_cursor_mni,
						       world_cursor_nat,
						       label_coords ));
	    }
	    catch( NumberFormatException exception) {
		/* the code below is suboptimal (we could figure out
		   which one text field to update), but it's only run
		   in exceptional situations (and when speed is not an
		   issue because the user just typed something...) */ 
		x_field_mni.setText( myRound( world_cursor_mni.x));
		y_field_mni.setText( myRound( world_cursor_mni.y));
		z_field_mni.setText( myRound( world_cursor_mni.z));
		x_field_native.setText( myRound( world_cursor_nat.x));
		y_field_native.setText( myRound( world_cursor_nat.y));
		z_field_native.setText( myRound( world_cursor_nat.z));
		post_field.setText( myRound( label_coords.post));
		lat_field.setText( myRound( label_coords.lat));
		sup_field.setText( myRound( label_coords.sup));
	    }
	    /* NB: ActionEvents are not a subclass of InputEvent,
	       thus cannot be marked "consumed" ... 
	    */
	} // end of actionPerformed()


	/** This method only directly changes the textfields and not the viewports. */
	synchronized final public void positionChangeDetected( PositionEvent e) {
	    if (DEBUG_TRACE) System.out.println("\t\t\t\t*DVP$CoordFields.positionChangeDetected");

	    if (DEBUG_POSITIONCHANGED) System.out.print("\nDataVolumePanel$CoordFields -> positionChangeDetected");
	    if (DEBUG_HIGH) System.out.println(" with e:"+e);
	    else if (DEBUG_POSITIONCHANGED) System.out.println();

	    // alias them to local (stack) var-s for speed:
	    final Point3Dfloat 	world_cursor_mni= this.world_cursor_mni;
	    final Point3Dint 	voxel_cursor_mni= this.voxel_cursor_mni;//overwritten below!
	    final Point3Dfloat 	world_cursor_nat= this.world_cursor_nat;
	    final Point3Dint 	voxel_cursor_nat= this.voxel_cursor_nat;//overwritten below!
	    final LabelCoords 	label_coords= this.label_coords;
	    String 		anatomy_label;

	    //Adjust the world_cursor's and label_coords to the new PositionEvent
	    if( e.isXmniChanged())
		world_cursor_mni.x= e.getXmni();
	    if( e.isYmniChanged())
		world_cursor_mni.y= e.getYmni();
	    if( e.isZmniChanged())
		world_cursor_mni.z= e.getZmni();
	    if( e.isXnatChanged())
		world_cursor_nat.x= e.getXnat();
	    if( e.isYnatChanged())
		world_cursor_nat.y= e.getYnat();
	    if( e.isZnatChanged())
		world_cursor_nat.z= e.getZnat();
	    if( e.isPostChanged())
		label_coords.post= e.getPost();
	    if( e.isSupChanged())
		label_coords.sup= e.getSup();
	    if( e.isLatChanged())
		label_coords.lat= e.getLat();

	    // Update the voxel cursors:
	    // (No need to sync the mni/native/label world cursors - this is ensured when creating)
	    if (DEBUG_TRACE) System.out.println("\t\t\t\t**DVP$CoordFields.positionChangeDetected -> _world2voxel_mni");
	    _world2voxel_mni( voxel_cursor_mni, world_cursor_mni);
	    if (DEBUG_TRACE) System.out.println("\t\t\t\t**DVP$CoordFields.positionChangeDetected -> _world2voxel_nat");
	    _world2voxel_nat( voxel_cursor_nat, world_cursor_nat);

	    //////////////////////////////////////////////////////////
	    // Reset the textfields to display the new cursor position:

	    //label_coords and label textfields:
	    if( e.isPostChanged())
		post_field.setText( myRound( label_coords.post));
	    if( e.isSupChanged())
		sup_field.setText( myRound( label_coords.sup));
	    if( e.isLatChanged())
		lat_field.setText( myRound( label_coords.lat));
	    if (DEBUG_TRACE) System.out.println("\t\t\t\t**DVP$CoordFields.positionChangeDetected -> _getLabelValue");
	    anatomy_label= _getLabelValue( world_cursor_mni);
	    anatomy_label_field.setText(anatomy_label);

	    //intensity:
	    if( intensity_value_field != null) {
		int new_voxel_value;
		if(!isNative) {
			if (DEBUG_TRACE) System.out.println("\t\t\t\t**DVP$CoordFields.positionChangeDetected -> _getVoxelValue(_world2voxel_common())");
			new_voxel_value= _getVoxelValue( _world2voxel_common( world_cursor_mni));
		}
		else {
			if (DEBUG_TRACE) System.out.println("\t\t\t\t**DVP$CoordFields.positionChangeDetected -> _getVoxelValue(_world2voxel_common())");
			new_voxel_value= _getVoxelValue( _world2voxel_nat( world_cursor_nat));
		}
		intensity_value_field.setText( _voxel2string( new_voxel_value));
	    }


	    // mni and native textfields, according to voxel or world type:
	    if( VOXEL_COORDINATES == coordinates_type) {
		/* == VOXEL COORDINATES == */
		if( e.isXmniChanged())
		    x_field_mni.setText( String.valueOf( voxel_cursor_mni.x));
		if( e.isYmniChanged())
		    y_field_mni.setText( String.valueOf( voxel_cursor_mni.y));
		if( e.isZmniChanged())
		    z_field_mni.setText( String.valueOf( voxel_cursor_mni.z));
		if( e.isXnatChanged())
		    x_field_native.setText( String.valueOf( voxel_cursor_nat.x));
		if( e.isYnatChanged())
		    y_field_native.setText( String.valueOf( voxel_cursor_nat.y));
		if( e.isZnatChanged())
		    z_field_native.setText( String.valueOf( voxel_cursor_nat.z));
	    }
	    else {
		/* == WORLD COORDINATES == */
		if( e.isXmniChanged())
		    x_field_mni.setText( myRound(world_cursor_mni.x));
		if( e.isYmniChanged())
		    y_field_mni.setText( myRound( world_cursor_mni.y));
		if( e.isZmniChanged())
		    z_field_mni.setText( myRound( world_cursor_mni.z));
		if( e.isXnatChanged())
		    x_field_native.setText( myRound( world_cursor_nat.x));
		if( e.isYnatChanged())
		    y_field_native.setText( myRound( world_cursor_nat.y));
		if( e.isZnatChanged())
		    z_field_native.setText( myRound( world_cursor_nat.z));
	    }


	    //Override unknown textfields if there was no native2mni transform
	    if (CoordConv.MNI2NAT == null) {
		if (e.source_type != PositionEvent.MNI_EVENT) {
			x_field_native.setText("-");
			y_field_native.setText("-");
			z_field_native.setText("-");
			x_field_native.setBackground(Color.YELLOW);
			y_field_native.setBackground(Color.YELLOW);
			z_field_native.setBackground(Color.YELLOW);
		}
		else {  // don't need MNI2NAT to show NATIVE generated event
			x_field_native.setBackground(Color.WHITE);
			y_field_native.setBackground(Color.WHITE);
			z_field_native.setBackground(Color.WHITE);
		}
	    }
	    if (CoordConv.NAT2MNI == null) {
		if (e.source_type == PositionEvent.NAT_EVENT) {
//			(this.!isNative)
			x_field_mni.setText("-");
			y_field_mni.setText("-");
			z_field_mni.setText("-");
			post_field.setText("-");
			sup_field.setText("-");
			lat_field.setText("-");
			anatomy_label_field.setText("No Native2MNI transform");
			x_field_mni.setBackground(Color.YELLOW);
			y_field_mni.setBackground(Color.YELLOW);
			z_field_mni.setBackground(Color.YELLOW);
			post_field.setBackground(Color.YELLOW);
			sup_field.setBackground(Color.YELLOW);
			lat_field.setBackground(Color.YELLOW);
			anatomy_label_field.setBackground(Color.YELLOW);
			x_field_native.setBackground(Color.WHITE);
			y_field_native.setBackground(Color.WHITE);
			z_field_native.setBackground(Color.WHITE);
		}
		else {  // don't need NAT2MNI to show MNI or LABEL generated event
			x_field_mni.setBackground(Color.WHITE);
			y_field_mni.setBackground(Color.WHITE);
			z_field_mni.setBackground(Color.WHITE);
			post_field.setBackground(Color.WHITE);
			sup_field.setBackground(Color.WHITE);
			lat_field.setBackground(Color.WHITE);
			anatomy_label_field.setBackground(Color.WHITE);
		}
	    }

	    //Override unknown textfields if there was no labels2mni transform
	    if (CoordConv.MNI2LABELS == null) {
		if (e.source_type != PositionEvent.LABEL_EVENT) {
			post_field.setText("-");
			sup_field.setText("-");
			lat_field.setText("-");
			anatomy_label_field.setText("No MNI2LABELS transform");
			post_field.setBackground(Color.YELLOW);
			sup_field.setBackground(Color.YELLOW);
			lat_field.setBackground(Color.YELLOW);
			anatomy_label_field.setBackground(Color.YELLOW);
		}
		else { // don't need MNI2LABELS to show LABEL generated event
			post_field.setBackground(Color.WHITE);
			sup_field.setBackground(Color.WHITE);
			lat_field.setBackground(Color.WHITE);
			anatomy_label_field.setBackground(Color.WHITE);
		}
	    }
	    if (CoordConv.LABELS2MNI == null) {
		if (e.source_type == PositionEvent.LABEL_EVENT) {
			x_field_mni.setText("-");
			y_field_mni.setText("-");
			z_field_mni.setText("-");
			x_field_mni.setBackground(Color.YELLOW);
			y_field_mni.setBackground(Color.YELLOW);
			z_field_mni.setBackground(Color.YELLOW);
			x_field_native.setText("-");
			y_field_native.setText("-");
			z_field_native.setText("-");
			x_field_native.setBackground(Color.YELLOW);
			y_field_native.setBackground(Color.YELLOW);
			z_field_native.setBackground(Color.YELLOW);
		}
		else { // don't need LABELS2MNI to show MNI or NATIVE generated event
			x_field_mni.setBackground(Color.WHITE);
			y_field_mni.setBackground(Color.WHITE);
			z_field_mni.setBackground(Color.WHITE);
			x_field_native.setBackground(Color.WHITE);
			y_field_native.setBackground(Color.WHITE);
			z_field_native.setBackground(Color.WHITE);
		}
	    }
	    if (CoordConv.label_array == null)
		anatomy_label_field.setText("No labels loaded");

	    if (DEBUG_TRACE) System.out.println("\t\t\t\t*DVP$CoordFields.positionChangeDetected DONE!\n");
	} // end of positionChangeDetected()
	
	final public int getMaxSliceNumber() { return -1; }
	final public float getOrthoStep() { return Float.NaN; }

	synchronized public void addPositionListener( PositionListener pl) {
	
	    if( null == event_listeners) 
		event_listeners= new Vector();
	    if( null == pl || event_listeners.contains( pl))
		return;
	    event_listeners.addElement( pl);
	}

	synchronized public void removePositionListener( PositionListener pl) {
	    
	    if( null != event_listeners) 
		event_listeners.removeElement( pl);
	}

	/** returns the current cursor positions (in world coordinates) */
	public Point3Dfloat getMNICursorPosition() { return world_cursor_mni; }
	public Point3Dfloat getNativeCursorPosition() { return world_cursor_nat; }
	public LabelCoords getLabelCoords() { return label_coords; }

	private String myRound(float num) {
		return String.valueOf( (float)(Math.round(num*10.0f)/10.0f));
	}


        /** world2voxel */
        final protected Point3Dint _world2voxel_mni( Point3Dfloat world_mni) {
		if(!isNative) {
			if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_mni -> local_sampling.world2voxel");
			return local_sampling.world2voxel( world_mni);
		}
		else {
			if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_mni -> CoordConv.world2voxel_mni");
			return CoordConv.world2voxel_mni( world_mni);
		}
        }
        private void _world2voxel_mni( Point3Dint voxel_mni, float wx, float wy, float wz) {
		if(!isNative) {
			if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_mni -> local_sampling.world2voxel");
			local_sampling.world2voxel( voxel_mni, wx, wy, wz);
		}
		else {
			if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_mni -> CoordConv.world2voxel_mni");
			CoordConv.world2voxel_mni( voxel_mni, wx, wy, wz);
		}
        }
        private void _world2voxel_mni( Point3Dint voxel_mni, Point3Dfloat world_mni) {
		if(!isNative) {
			if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_mni -> local_sampling.world2voxel");
			local_sampling.world2voxel( voxel_mni, world_mni);
		}
		else {
			if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_mni -> CoordConv.world2voxel_mni");
			CoordConv.world2voxel_mni( voxel_mni, world_mni);
		}
        }
        private Point3Dint _world2voxel_nat( Point3Dfloat world_nat) {
		if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_nat -> CoordConv.world2voxel_nat");
		return CoordConv.world2voxel_nat( world_nat);
        }
        private void _world2voxel_nat( Point3Dint voxel_nat, float wx, float wy, float wz) {
		if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_nat -> CoordConv.world2voxel_nat");
		CoordConv.world2voxel_nat( voxel_nat, wx, wy, wz);
        }
        private void _world2voxel_nat( Point3Dint voxel_nat, Point3Dfloat world_nat) {
		if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_nat -> CoordConv.world2voxel_nat");
		CoordConv.world2voxel_nat(voxel_nat, world_nat);
        }

        private Point3Dint _world2voxel_common( Point3Dfloat world) {
		if (DEBUG_VW) System.out.println("DataVolumePanel.w2vox_common -> CoordConv.world2voxel_common");
		return CoordConv.world2voxel_common( world);
        }

        /** voxel2world */
        private void _voxel2world_mni( Point3Dfloat world_cursor_mni, Point3Dint voxel_cursor_mni) {
		if(!isNative) {
			if (DEBUG_VW) System.out.println("DataVolumePanel.vox2w_mni -> local_sampling.voxel2world");
			local_sampling.voxel2world(world_cursor_mni, voxel_cursor_mni);
		}
		else {
			if (DEBUG_VW) System.out.println("DataVolumePanel.vox2w_mni -> CoordConv.voxel2world_mni");
			CoordConv.voxel2world_mni(world_cursor_mni, voxel_cursor_mni);
		}
        }
        private void _voxel2world_nat( Point3Dfloat world_nat, Point3Dint voxel_nat) {
		if (DEBUG_VW) System.out.println("DataVolumePanel.vox2w_nat -> CoordConv.voxel2world_nat");
		CoordConv.voxel2world_nat( world_nat, voxel_nat);
        }





	public String toString(){
		if (DEBUG)
			return getTitle()+" - DataVolumePanel$CoordFields [isNative:"+isNative+",cursor:"+world_cursor_mni+"]";
		else
			return getTitle()+" - DataVolumePanel$CoordFields";
	}

    } // end of class CoordinateFields

    public String toString(){
	return getTitle()+" - DataVolumePanel [isNative:"+isNative+",cursor:"+global_cursor_mni+"]";
    }

} // end of class DataVolumePanel
