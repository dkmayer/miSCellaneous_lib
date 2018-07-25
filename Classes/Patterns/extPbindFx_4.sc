
/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2018-07-25, version 0.21
	Copyright (C) 2009-2018 Daniel Mayer
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


// subroutines used to establish a proper ordering of
// fx synths, zero synths, split synths and zero synths for split synths

+Integer {

	// receiver is index
	// find the right position for zero synth(s) #i (related to inbus of fx # i),
	// this order is established before fitting in fx synths

	miSC_zeroIdRank { |firstFxIds, lastFxIds, nodeId, groupId|
		var idAtTail = false, j, referenceId, actionNum;
		firstFxIds[this].isNil.if {
			j = this + 1;
			while { (j <= (firstFxIds.size-1)) and: { idAtTail.not } }{
				firstFxIds[j].notNil.if {
					referenceId = firstFxIds[j];
					idAtTail = true;
					actionNum = 2;
				};
				j = j + 1;
			};
			idAtTail.not.if {
				referenceId = groupId;
				actionNum = 1;
			};
			lastFxIds[this] = nodeId;
		}{
			referenceId = firstFxIds[this];
			actionNum = 2;
		};
		firstFxIds[this] = nodeId;
		^[firstFxIds, lastFxIds, referenceId, actionNum]
	}


	// receiver is index
	// find the right position for split and fx synth(s) #i

	miSC_fxIdRank { |firstFxIds, lastFxIds, nodeId, groupId|
		var idAtHead = false, j, referenceId, actionNum;
		lastFxIds[this].isNil.if {
			j = this - 1;
			while { (j >= 0) and: { idAtHead.not } }{
				lastFxIds[j].notNil.if {
					referenceId = lastFxIds[j];
					idAtHead = true;
				};
				j = j - 1;
			};
			idAtHead.not.if { referenceId = groupId };
			lastFxIds[this] = nodeId;
		}{
			referenceId = lastFxIds[this];
		};
		actionNum = 3;
		lastFxIds[this] = nodeId;
		^[firstFxIds, lastFxIds, referenceId, actionNum]
	}

}

		
