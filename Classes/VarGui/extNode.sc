
/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2017-09-26, version 0.18
	Copyright (C) 2009-2017 Daniel Mayer
	Email: 	daniel-mayer@email.de
	URL:	http://daniel-mayer.at

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

// doubling, keep extension for Integer private

+Node {

	miSC_runMsg { arg flag=true;
		^[12, nodeID,flag.binaryValue]; 
	}

	miSC_setMsg { arg ... args;
		^[15, nodeID] ++ args.asOSCArgArray; 	 
	}	
	
	miSC_setnMsg { arg ... args;
		^[16, nodeID] ++ Node.setnMsgArgs(*args);
	}
	
	miSC_freeMsg { ^[11, nodeID] }
	
}

