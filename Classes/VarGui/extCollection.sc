
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



+Collection {
	
	miSC_enumerateIndices {
		(this.size == 1).if {
			^(0..(this[0]-1));
		}{
			^[(0..(this[0]-1)), this.copyToEnd(1).miSC_enumerateIndices]
				.allTuples.collect(_.flatten)
		}
	}
	
	miSC_isGrouping {|n|
		var f;
		this.every({|x| x.isKindOf(Collection) and: { x.every({|y| y.isInteger})} }).not.if { ^false };
		f = this.asSequenceableCollection.collect(_.asSequenceableCollection).flatten;
		((f.size == n) and: { f.every({|x| (x>=0) && (x<n) })}).not.if { ^false };
		^true;
	}

	miSC_collectCopy { ^this.collect(_.miSC_collectCopy) }

	miSC_asSet { ^this.asSet }
}
