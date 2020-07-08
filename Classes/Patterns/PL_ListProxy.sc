
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

PL_ListProxy {
	var <>symbol, <>listSource, <>streamSources, <>index, 
		<>itemHasChanged, <>listHasChanged, <>wrongList, <>envir;
	
	*new { |thing, envir = \current|
		^(thing.isKindOf(Symbol)).if {
			super.new.symbol_(thing).envir_(envir.miSC_getEnvir)
		}{
			thing.isKindOf(SequenceableCollection).not.if {
				SimpleInputError("List input of PL_ListPattern must be SequenceableCollection.").throw;
			}{
				super.new.listSource_(thing).streamSources_(thing)
			}
		};
	}

	
	get { ^(symbol.isNil).if { listSource }{ envir[symbol] } }

	update { |i|
		var currentList, j, k;
		symbol.notNil.if {
			currentList = this.get;
			currentList.isKindOf(SequenceableCollection).not.if {
				(currentList !== wrongList).if {
					("Embedding of PL_ListPattern encountered reference ('" ++ 
						symbol.asString ++ "') to an item not kind of a SequenceableCollection, " ++
						"reference ignored.").warn;
					wrongList = currentList
				}
			}{
				(currentList !== listSource).if {
					listHasChanged = index.notNil;
					listSource = currentList;
					streamSources = listSource.copy;
				}{
					i.isNil.not.if {
						j = i % currentList.size;
						k = i % streamSources.size;
						(currentList[j] !== streamSources[k]).if {
							itemHasChanged = true;
							streamSources[k] = currentList[j];
						}{
							itemHasChanged = false;
						}
					}
				};
			}
		}{
			index.isNil.if { listHasChanged = false; itemHasChanged = false }
		};
		index = i;
		^this;
	}

	do { |function, ref = (`true)|
		var i = 0, size = this.streamSources.size;
		while { 
			(i < size) and: { ref.() }
		} {
			function.value(this.source(i), i);
			i = i + 1;
		}
	}

	doN { |function, n, ref = (`true)|
		var i = 0;
		while { 
			(i < n) and: { ref.() }
		} {
			function.value(this.source(i), i);
			i = i + 1;
		}
	}

	reverseDo { |function, ref = (`true)|
		var size = this.streamSources.size, i, j = 0;
		i = size - 1; 
		while { 
			(i >= 0) and: { ref.() }
		} {
			function.value(this.source(i), j);
			i = i - 1;
			j = j + 1;
		}
	}

	reverseDoN { |function, n, ref = (`true)|
		var size = this.streamSources.size, i, j = 0;
		i = size - 1; 
		while { 
			(j < n) and: { ref.() }
		} {
			function.value(this.source(i), j);
			i = i - 1;
			j = j + 1;
		}
	}


	size { ^this.listSource.size }

	value { |i| ^streamSources.wrapAt(i).value }

	source { |i, doWrap = true| 
		(streamSources.size > 0).if {
			^streamSources.perform(doWrap.if { \wrapAt }{ \at }, i) 
		}{
			Error("PL_ListPattern requires list or reference to a " ++
				"non-empty collection; received " ++ streamSources ++ ".").throw;
		}
	}

}


PL_Proxy {
	var <>symbol, <>streamSource, <>itemHasChanged, <>envir;
	
	*new { |thing, envir = \current, exclude = #[]|
		^((thing.isKindOf(Symbol)) && (exclude.includes(thing).not)).if {
			super.new.symbol_(thing).envir_(envir.miSC_getEnvir)
		}{
			super.new.streamSource_(thing)
		}
	}
	
	get { ^(symbol.isNil).if { streamSource }{ envir[symbol] } }

	update { 
		var currentItem;
		symbol.notNil.if {
			currentItem = this.get;
			itemHasChanged = (currentItem !== streamSource).if { 
				streamSource = currentItem; 
				true;
			}{
				false	
			};
		}{
			itemHasChanged = false;
		};
		^this
	}

	value { this.update; ^streamSource.value }

	source { this.update; ^streamSource }
}



