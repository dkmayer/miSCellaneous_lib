
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

+ SequenceableCollection {

	miSC_isKeysAndValues { |requiredKeys|
		^this.size.even and: { this.select({|x,i| i.even }).every({|x| 
			(x.isKindOf(Symbol) || x.isKindOf(String)) && (requiredKeys.isNil or: { requiredKeys.includes(x.asSymbol) })     
		})} 
	}
	
	miSC_isPossiblePbindData {
		^this.miSC_isKeysAndValues or: { 
			this.every({|x| x.isKindOf(SequenceableCollection) and: { x.miSC_isKeysAndValues }})
		}
	}
	
	miSC_isPossiblePbindArgs {
		^this.size.even and: {
			this.every({|x,i| 
				(i.even and: { (x.isKindOf(Number) and: { x > 0 }) or: { x.isKindOf(Pattern) or: { x.isKindOf(Stream) }}}) or: { 
					i.odd and: { x.isKindOf(SequenceableCollection) and: { x.miSC_isPossiblePbindData } }  
				}
			}) 
		}
	}
		
	miSC_hasNoReservedKeys { |reservedKeys| // suppose this.miSC_isKeysAndValues was true
		^(this.select({|x,i| i.even }).asSet & (reservedKeys ?? Set[])).size == 0
	}
	
	miSC_hasNoReservedKeysInPbindData { |reservedKeys| // suppose this.miSC_isPossiblePbindData was true
		^(this.miSC_isKeysAndValues and: { this.miSC_hasNoReservedKeys(reservedKeys) }) or: { 
			this.every({|x| x.isKindOf(SequenceableCollection) and: { x.miSC_hasNoReservedKeys(reservedKeys) }})
		}
	}
	
	miSC_hasNoReservedKeysInPbindArgs { |reservedKeys| // suppose this.miSC_isPossiblePbindArgs was true
		^this.every({|x,i| 
			i.even or: { x.miSC_hasNoReservedKeysInPbindData(reservedKeys) }		}) 
	}

	miSC_isPossibleHelpSynthArgs { |requiredKeys|
		^this.miSC_isKeysAndValues(requiredKeys) and: { this.every({|x,i| 
			(i.odd and: { x.isKindOf(Number) or: { x.isKindOf(Function) }}) || i.even   
			})
		}
	}

	miSC_isPossibleHelpSynthParArgs { |requiredKeys|
		^this.miSC_isKeysAndValues(requiredKeys) and: { this.every({|x,i| 
			(i.odd and: { x.isKindOf(Number) or: { x.isKindOf(Pattern) or: { x.isKindOf(Stream) }}}) || i.even   
			})
		}
	}
	
	miSC_areControlRateSynthDefs {
		^this.every({|x| x.children.last.rate == \control })
	}
	
	miSC_isIndexSubset {|size|
		^(this.asSet.size == this.size) && this.every({|x| x.isKindOf(Integer) && (x >= 0) && (x <size) })
	}
}
	
