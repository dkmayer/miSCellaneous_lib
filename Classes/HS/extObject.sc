
/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2020-07-08, version 0.24
	Copyright (C) 2009-2020 Daniel Mayer
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


// input checks

+ Object {

	miSC_isKindOfFixedHSindex { |size|
		^(this.isKindOf(Integer) and: { (this >= 0) && (this < size) }) || 
			[\switch, \all].includes(this.asSymbol)
	} 
	 
	miSC_isKindOfFixedHSindices { |size|
		^this.miSC_isKindOfFixedHSindex(size) || (
			this.isKindOf(SequenceableCollection) and: { 
				this.every({ |x| 
					x.isKindOf(Integer) && (x >= 0) && (x < size)
				}) && 
				(this.asSet.size == this.size) 
			}
		)
	}
		
	miSC_isKindOfPossibleHSindices { |demandNum, hsNum|
		^this.isKindOf(SequenceableCollection) and: {
			(this.size == demandNum) &&
				this.every({|x|
					x.isKindOf(Pattern) || x.isKindOf(Stream) || x.miSC_isKindOfFixedHSindices(hsNum)
				})
		}
	}

	miSC_isKindOfPossibleHSstartIndices { |size|
		^(this.isKindOf(SequenceableCollection) and: { this.miSC_isIndexSubset(size) }) ||
			(this.isKindOf(Integer) and: { this <= (size-1) }) ||			[\none, \all].includes(this.asSymbol)
	} 
	
	miSC_normalizeHSstartIndices { |size|  // suppose this.miSC_isKindOfPossibleHSstartIndices was true
		case  
			{ this == \all }{ ^(0..(size-1)) }
			{ this == \none }{ ^[] }
			{ true }{ ^this.asCollection }
	} 
}		
		
