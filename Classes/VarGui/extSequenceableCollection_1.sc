
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

+SequenceableCollection {

	miSC_isGrouping { |n|
		var f;
		this.every { |x| 
			x.isKindOf(Collection) and: { x.every {|y| y.isInteger} } 
		}.not.if { ^false };
		f = this.collect(_.asSequenceableCollection).flatten.asSet;
		((f.size == n) and: { f.every {|x| (x >= 0) && (x < n) } }).not.if { ^false };
		^true
	}
	
	miSC_isConnected {
		// expects grouping
		var y;
		this.every { |x| 
			y = x.sort; 
			(y.size > 0).if { 
				(y.last - y.first + 1) == y.size
			}{ 
				true  
			}
		}.if { ^true }{ ^false }; 
	}

	miSC_groupingFromIndices {
		var max = this.maxItem, groups;
		groups = Array.newClear(max + 1);
		this.do { |x,i| groups[x] = groups[x].add(i) };
		^groups
	}
	
	
}
