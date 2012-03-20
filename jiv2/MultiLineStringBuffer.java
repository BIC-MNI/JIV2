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
 * A buffer for multi-line text.
 *
 * @author Chris Cocosco, Lara Bailey (bailey@bic.mni.mcgill.ca)
 * @version $Id: MultiLineStringBuffer.java,v 2.0 2010/02/21 11:20:41 bailey Exp $
 */
public class MultiLineStringBuffer {

    /*protected*/ static final String 	ls= System.getProperty( "line.separator");
    
    /*private*/ StringBuffer 		str_buff= new StringBuffer();
    /*private*/ int 			line_count= 0;
    /*private*/ int 			max_column= 0;

    /**
     * @return all the text is returned as one string, with the lines 
     * properly separated by the 'newline' of runtime platform.
     */
    public String toString() { return str_buff.toString(); }

    /**
     * @param line should not contain the end-of-line (newline) character.
     */
    public synchronized MultiLineStringBuffer append_line( String line ) {

	str_buff.append( line + ls);
	++line_count;
	max_column= Math.max( line.length(), max_column);
	return this;
    }

    public int getLineCount() { return line_count; }

    /**
     * @return length of the longest line (that is, 
     * column numbers start at 1, not at 0)
     */
    public int getMaxColumn() { return max_column; }

} // end of class MultiLineStringBuffer
