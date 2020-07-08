
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


+Object {

	miSC_checkFxOrder { ^false }

	miSC_fxOrderWarnString {
		^"wrong fxOrder data type: \n" ++
		"It must be an Integer, SequenceableCollection or IdentityDictionary, \n" ++
		"see PbindFx help for details and examples.\n"
	}

	miSC_getFxOrderData { |fxDataSize, maxSplitSize = 8, doCheck = true|

		// this makes an input check if doCheck == true

		// receiver can be
		// (1) an Integer indicating an effect (or source)
		// (2) a SequenceableCollection indicating an effect chain
		// (3) an IdentityDictionary indicating an effect graph,

		// output is an array [x,y,z] with
		// x: topological ordering of graph,
		// this is either [0] (no effect) or an array of fx indices where 0 as head is dropped

		// y: an IdentityDictionary of predesessors (node \o added for 'out')
		// z: an IdentityDictionary of successors (node \o added for 'out')
		// main job is case (3)

		^doCheck.if {
			this.miSC_checkFxOrder(fxDataSize, maxSplitSize)
		}{
			true
		}.if {

			this.miSC_getFxOrders;
		}{
			"".postln; "NO FX APPLIED".warn;
			this.miSC_fxOrderWarnString.postln;
			[[0], IdentityDictionary[\o -> 0], IdentityDictionary[0 -> \o]]
		}
	}
}


+Integer {

	miSC_checkFxOrder { |maxIndex| ^(this >= 0) and: { this <= maxIndex } }

	miSC_fxOrderWarnString {
		^"fxOrder given as Integer failed: \n" ++
		"It must be greater or equal 0 and smaller or equal size of fxData, \n" ++
		"see PbindFx help for other possibilities of definition and examples.\n"
	}

	miSC_getTopoOrder {
		^(this == 0).if { [0] }{ [this] }
	}

	miSC_getPredecessors {
		^(this == 0).if {
			IdentityDictionary[\o -> 0]
		}{
			IdentityDictionary[\o -> this, this -> 0]
		}
	}

	miSC_getSuccessors {
		^(this == 0).if {
			IdentityDictionary[0 -> \o]
		}{
			IdentityDictionary[0 -> this, this -> \o]
		}
	}

	miSC_getFxOrders {
		^[this.miSC_getTopoOrder, this.miSC_getPredecessors, this.miSC_getSuccessors]
	}
}

+SequenceableCollection {

	miSC_checkFxOrder { |maxIndex|
		^this.every { |x,i|
			x.isInteger and: { (x >= 1) or: { (i == 0) and: { x == 0 } }
				and: { x <= maxIndex }
			}
		}
	}

	miSC_fxOrderWarnString {
		^"fxOrder given as SequenceableCollection failed: \n" ++
		"It must contain Integers greater or equal 1 and smaller " ++
		"or equal size of fxData, \n" ++
		"see PbindFx help for other possibilities of definition and examples.\n"
	}

	miSC_getTopoOrder {
		^(this[0] == 0).if {
			(this.size == 1).if { [0] }{ this.drop(1) }
		}{
			this
		}
	}

	miSC_getPredecessors {
		var dict = IdentityDictionary();
		this.do { |x,i|
			(i != 0).if { dict.put(x, this[i-1]) }
		};
		dict.put(this[0], 0);
		dict.put(\o, this.last);
		^dict
	}

	miSC_getSuccessors {
		var dict = IdentityDictionary();
		this.do { |x,i|
			dict.put(x, (i != (this.size-1)).if { this[i+1] }{ \o })
		};
		dict.put(0, this[0]);
		^dict
	}

	miSC_getFxOrders {
		^[this.miSC_getTopoOrder, this.miSC_getPredecessors, this.miSC_getSuccessors]
	}
}


+IdentityDictionary {

	miSC_checkFxOrder { |maxIndex, maxSplitSize|
		^this.miSC_keysValuesEvery { |k,v,i|
			k.isInteger and: { k >= 0 } and: { k <= maxIndex } and: {
				(v.isInteger and: { v >= 1 } and: { v <= maxIndex }) or: { v == \o } or: {
					v.isKindOf(Collection) and: {
						v.every { |x|
							(x.isInteger and: { x >= 1 } and: { x <= maxIndex }) or:
							{ x == \o }
						} and: {
							v.size == v.asSet.size
						} and: {
							v.size <= maxSplitSize
						}
					}
				}
			}
		}
	}

	miSC_fxOrderWarnString {
		^"fxOrder given as IdentityDictionary failed: \n" ++
		"Keys must be Integers greater or equal 0 and smaller or equal size of " ++
		"fxData, \nvalues must be Integers greater or equal 1 and smaller " ++
		"or equal size of fxData or Symbol \o or \n" ++
		"SequenceableCollection thereof, see PbindFx help for other possibilities " ++
		"of definition and examples.\n"
	}

	miSC_keysValuesEvery { |function|
		this.keysValuesDo { |k,v,i| if (function.value(k,v,i).not) { ^false } }
		^true;
	}

	miSC_keysValuesAny { |function|
		this.keysValuesDo { |k,v,i| if (function.value(k,v,i)) { ^true } }
		^false;
	}

	miSC_putAppend { |k, v|
		this[k].isNil.if {
			this.put(k,v)
		}{
			this[k].isKindOf(Collection).if {
				this[k] = this[k].add(v)
			}{
				this.put(k, [this[k], v])
			}
		};
		^this
	}

	miSC_removeDrop { |k, v|
		this[k].isNil.if {
			this
		}{
			(this[k].isKindOf(Collection) and: { this[k].size >= 2 }).if {
				this[k].remove(v)
			}{
				this.put(k, nil)
			}
		};
		^this
	}

	// already checked: well-defined
	miSC_getPredecessors {
		var dict = IdentityDictionary.new;
		this.keysValuesDo { |k, v|
			v.isKindOf(Collection).if {
				v.do { |x| dict.miSC_putAppend(x, k)  }
			}{
				dict.miSC_putAppend(v, k)
			}
		};
		^dict
	}

	// adds \o (~ out) to nodes, which have no successors
	miSC_getSuccessors {
		var graph = this.as(IdentityDictionary).deepCopy, vals;
		vals = graph.values;

		vals.do { |v|
			v.isKindOf(Collection).if {
				v.do { |w|
					(graph[w].isNil and: { w != \o }).if {
						graph.miSC_putAppend(w, \o)
					}
				}
			}{
				(graph[v].isNil and: { v != \o }).if { graph.miSC_putAppend(v, \o) }
			}
		}
		^graph
	}

	// we suppose there are no doubled edges, this has been checked in miSC_checkFxOrder
	miSC_getTopoOrder { |predecessors, successors|
		var graph, node, order, nodesWithoutIns;

		graph = successors ?? { this.miSC_getSuccessors };
		predecessors = predecessors ?? { graph.miSC_getPredecessors };

		// if 0 is in graph, we want an ordering with it at head
		graph.keys.includes(0).if {
			// already checked that 0 is no successor
			 nodesWithoutIns = nodesWithoutIns.add(0)
		};

		graph.keysValuesDo { |k,v|
			(k != 0).if {
				predecessors[k].isNil.if { nodesWithoutIns = nodesWithoutIns.add(k) }
			}
		};

		while { nodesWithoutIns.size != 0 }{
			node = nodesWithoutIns[0];
			nodesWithoutIns = nodesWithoutIns.drop(1);
			order = order.add(node);
			graph[node].asArray.do { |succ|
				predecessors.miSC_removeDrop(succ, node);
				predecessors[succ].isNil.if {
					nodesWithoutIns = nodesWithoutIns.add(succ)
				};
			};
			graph.put(node, nil);
		};
		^(graph.size == 0).if { order.reject(_==\o) }
	}

	miSC_getFxOrders {
		var order, predecessors, successors, col;

		successors = this.miSC_getSuccessors;
		predecessors = successors.miSC_getPredecessors;

		order = this.miSC_getTopoOrder(predecessors.deepCopy, successors.deepCopy);
		((order.size > 1) and: (order[0] == 0)).if { order.remove(0) };

		^order.isNil.if {
			"".postln; "NO FX APPLIED".warn;
			"cyclic graph !".postln;
			[[0], IdentityDictionary[\o -> 0], IdentityDictionary[0 -> \o]]
		}{
			[order, predecessors, successors]
		}
	}
}


