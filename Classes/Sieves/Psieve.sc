
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


Psieve : Pattern {

	classvar <>limit = 65536;

	var <>items, <>offsets, <>limit, <>maxLength,
		<>withOffsets = false, <>doSum = false, <>op, <>difIndex;


	embedInStream { |inval|
		var goOn = true, last, count = 0, counts, itemStreams, opStream, nextOp,
		yielder, next, maxLengthNum, minIndices, min, leftIndices, counter,
		difIndexStream, nextDifIndex, indicesQueue = PriorityQueue.new,
		// faster than storing indices in an array
		minIndicesDict = IdentityDictionary.new;

		offsets.isNil.if { offsets = 0!(items.size) };

		// if Sieve is passed adjust offset
		items.do { |item, i|
			item.isKindOf(Sieve).if {
				// set to nil with empty Sieve
				offsets[i] = (item.mode == \points).if {
					(item.list.size != 0).if { offsets[i] + item.list[0] };
				}{
					item.offset.notNil.if { offsets[i] + item.offset };
				}
			}
		};

		itemStreams = items.collect(_.miSC_streamifySieveItems);
		counts = offsets;
		counts.do { |offset, i|
			((offset.notNil) and: { offset <= limit }).if {
				indicesQueue.put(offset, i);
				leftIndices = leftIndices.add(i);
			}
		};

		opStream = op.asStream;
		difIndexStream = difIndex.asStream;
		maxLengthNum = maxLength.value;

		leftIndices.notNil.if { while { goOn }{

			// partial sums stored in PriorityQueue
			min = indicesQueue.topPriority.round.asInteger;
			minIndices = indicesQueue.pop;

			// if more than one min is found, indices are collected in Dictionary
			while { indicesQueue.topPriority == min }{
				minIndices.isInteger.if {
					minIndicesDict.clear;
					minIndices = minIndicesDict.put(minIndices, true)
				};
				minIndicesDict.put(indicesQueue.pop, true)
			};

			// yielder function chooses output type
			yielder = {
				doSum.if {
					inval = min.yield;
					last = min;
					count = count + 1;
				}{
					last.notNil.if {
						inval = (min - last).yield;
						count = count + 1;
					};
					last = min
				}
			};

			nextOp = opStream.next;
			nextOp.isNil.if { ^inval };

			// operator decides what happens at this partial sum
			case
				{ nextOp == \u }
				{ yielder.() }
				{ nextOp == \s }
				{ (minIndices.size == items.size) if: { yielder.() } }
				{ nextOp == \sd }
				{ minIndices.isInteger if: { yielder.() } }
				{ nextOp == \d }
				{
					// select the receiver of logical difference
					difIndex.isNil.if {
						nextDifIndex = 0
					}{
						nextDifIndex = difIndexStream.next;
						nextDifIndex.isNil.if { ^inval };
					};
					(minIndices == nextDifIndex) if: { yielder.() }
				};

			// faster than minIndices.asArray.do { ...
			counter = { |i|
				next = itemStreams[i].next;
				next.notNil.if {
					counts[i] = counts[i] + next;
					(counts[i] <= limit).if { indicesQueue.put(counts[i], i) }
				};
				(next.isNil or: { counts[i] > limit }).if { leftIndices.remove(i) };
			};
			minIndices.isInteger.if { counter.(minIndices) }{ minIndices.keysDo(counter.(_)) };

			case
				{ nextOp == \u }
				{ (leftIndices.size == 0).if { goOn = false } }
				{ nextOp == \s }
				{ (leftIndices.size < (items.size)).if { goOn = false } }
				{ nextOp == \sd }
				{ (leftIndices.size == 0).if { goOn = false } }
				{ nextOp == \d }
				{ (leftIndices[0] != 0).if { goOn = false } };

			(count >= maxLengthNum).if { goOn = false }
		} };
		^inval
	}


	*miSC_checkOffsetGenList { |genList, limit|
		var offsets, items, classes = [Integer, Pattern, Stream, Sieve];
		genList.size.do { |i|
			i.even.if {
				items = items.add(genList[i])
			}{
				offsets = offsets.add(genList[i])
			}
		};
		items.every { |x| classes.any(x.isKindOf(_)) }.not.if {
			SimpleInitError(
				"Sieve base items must be Integers > 0, Sieves or: \n" ++
				"Patterns or Streams to make Integers > 0 (intervals)"
			).throw
		};
		items.any { |x| x.isKindOf(Integer) and: { x <= 0 } }.if {
			SimpleInitError("Integers as sieve bases must be > 0").throw
		};
		offsets.every { |x| x.isKindOf(Integer) }.not.if {
			SimpleInitError("Offsets must be Integers").throw
		};
		limit = limit ?? { this.limit };
		^[items, offsets, limit]
	}


	*miSC_checkGenList { |genList, limit|
		var items = genList, classes = [Integer, Pattern, Stream, Sieve];

		items.every { |x| classes.any(x.isKindOf(_)) }.not.if {
			SimpleInitError(
				"Sieve base items must be Integers > 0, Sieves or: \n" ++
				"Patterns or Streams to make Integers > 0 (intervals)"
			).throw
		};
		items.any { |x| x.isKindOf(Integer) and: { x <= 0 } }.if {
			SimpleInitError("Integers as sieve bases must be > 0").throw
		};
		limit = limit ?? { this.limit };
		^[items, limit]
	}
}


PSVunion : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, true, \u)
	}
}


PSVunion_o : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, true, \u)
	}
}

PSVunion_i : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, false, \u)
	}
}

PSVunion_oi : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, false, \u)
	}
}


PSVsect : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, true, \s)
	}
}


PSVsect_o : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, true, \s)
	}
}

PSVsect_i : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, false, \s)
	}
}

PSVsect_oi : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, false, \s)
	}
}



PSVsymdif : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, true, \sd)
	}
}


PSVsymdif_o : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, true, \sd)
	}
}

PSVsymdif_i : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, false, \sd)
	}
}

PSVsymdif_oi : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, false, \sd)
	}
}



PSVdif : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, true, \d)
	}
}


PSVdif_o : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, true, \d)
	}
}

PSVdif_i : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, false, \d)
	}
}

PSVdif_oi : Psieve {
	*new { |genList, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, false, \d)
	}
}


PSVop : Psieve {
	*new { |genList, op = \u, difIndex = 0, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, true, op, difIndex)
	}
}


PSVop_o : Psieve {
	*new { |genList, op = \u, difIndex = 0, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, true, op, difIndex)
	}
}

PSVop_i : Psieve {
	*new { |genList, op = \u, difIndex = 0, maxLength = inf, limit|
		var items, lim;
		#items, lim = this.miSC_checkGenList(genList, limit);
		^super.newCopyArgs(items, nil, lim, maxLength, false, false, op, difIndex)
	}
}

PSVop_oi : Psieve {
	*new { |genList, op = \u, difIndex = 0, maxLength = inf, limit|
		var items, offsets, lim;
		#items, offsets, lim = this.miSC_checkOffsetGenList(genList, limit);
		^super.newCopyArgs(items, offsets, lim, maxLength, true, false, op, difIndex)
	}
}


