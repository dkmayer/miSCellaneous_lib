
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


Sieve {

	classvar <>limit = 65536;

	var <list, <mode = \intervals, <offset;

	*newEmpty { ^super.new.miSC_setList(List.new) }

	*new { |gen, limit| ^Sieve.union(gen, `(limit ?? { this.limit })) }
	*new_o { |gen, offset, limit| ^Sieve.union_o(gen, offset, `(limit ?? { this.limit })) }
	*new_i { |gen, limit| ^Sieve.union_i(gen, `(limit ?? { this.limit })) }
	*new_oi { |gen, offset, limit| ^Sieve.union_oi(gen, offset, `(limit ?? { this.limit })) }

	*union { |...data| ^this.newEmpty.miSC_sieveOp(data, false, true, \u) }
	*union_o { |...data| ^this.newEmpty.miSC_sieveOp(data, true, true, \u) }
	*union_i { |...data| ^this.newEmpty.miSC_sieveOp(data, false, false, \u) }
	*union_oi { |...data| ^this.newEmpty.miSC_sieveOp(data, true, false, \u) }

	*sect { |...data| ^this.newEmpty.miSC_sieveOp(data, false, true, \s) }
	*sect_o { |...data| ^this.newEmpty.miSC_sieveOp(data, true, true, \s) }
	*sect_i { |...data| ^this.newEmpty.miSC_sieveOp(data, false, false, \s)	}
	*sect_oi { |...data| ^this.newEmpty.miSC_sieveOp(data, true, false, \s)	}

	*symdif { |...data| ^this.newEmpty.miSC_sieveOp(data, false, true, \sd) }
	*symdif_o { |...data| ^this.newEmpty.miSC_sieveOp(data, true, true, \sd) }
	*symdif_i { |...data| ^this.newEmpty.miSC_sieveOp(data, false, false, \sd) }
	*symdif_oi { |...data| ^this.newEmpty.miSC_sieveOp(data, true, false, \sd) }

	*dif { |...data| ^this.newEmpty.miSC_sieveOp(data, false, true, \d) }
	*dif_o { |...data| ^this.newEmpty.miSC_sieveOp(data, true, true, \d) }
	*dif_i { |...data| ^this.newEmpty.miSC_sieveOp(data, false, false, \d) }
	*dif_oi { |...data| ^this.newEmpty.miSC_sieveOp(data, true, false, \d) }


	union { |...data| ^Sieve.union(this, *data) }
	union_o { |...data| ^Sieve.union_o(this, *data) }
	union_i { |...data| ^Sieve.union_i(this, *data) }
	union_oi { |...data| ^Sieve.union_oi(this, *data) }

	| { |gen| ^Sieve.union(this, gen) }
	|* { |gen| ^Sieve.union_i(this, gen) }


	sect { |...data| ^Sieve.sect(this, *data) }
	sect_o { |...data| ^Sieve.sect_o(this, *data) }
	sect_i { |...data| ^Sieve.sect_i(this, *data) }
	sect_oi { |...data| ^Sieve.sect_oi(this, *data) }

	& { |gen| ^Sieve.sect(this, gen) }
	&* { |gen| ^Sieve.sect_i(this, gen) }


	symdif { |...data| ^Sieve.symdif(this, *data) }
	symdif_o { |...data| ^Sieve.symdif_o(this, *data) }
	symdif_i { |...data| ^Sieve.symdif_i(this, *data) }
	symdif_oi { |...data| ^Sieve.symdif_oi(this, *data) }

	-- { |gen| ^Sieve.symdif(this, gen) }
	--* { |gen| ^Sieve.symdif_i(this, gen) }


	dif { |...data| ^Sieve.dif(this, *data) }
	dif_o { |...data| ^Sieve.dif_o(this, *data) }
	dif_i { |...data| ^Sieve.dif_i(this, *data) }
	dif_oi { |...data| ^Sieve.dif_oi(this, *data) }

	- { |gen| ^Sieve.dif(this, gen) }
	-* { |gen| ^Sieve.dif_i(this, gen) }


	plot { this.copy.toIntervals.list.as(Array).plot.plotMode_(\plines).refresh }
	size { ^list.size }

	// conversions change receiver
	toIntervals {
		var offset;
		(mode == \points).if {
			offset = (list.size != 0).if { list.first };
			this.miSC_setList(list.differentiate.drop(1))
				.miSC_setMode(\intervals).miSC_setOffset(offset)
		}{
			"Sieve already in mode 'intervals'"
			^this
		}
	}

	toPoints {
		var points;
		(mode == \intervals).if {
			points = (list.size != 0).if { List[offset] }{ List[] };
			list.do { |interval| var x = points.last + interval; points.add(x) };
			^this.miSC_setList(points).miSC_setMode(\points).miSC_setOffset(nil)
		}{
			"Sieve already in mode 'points'"
			^this
		}
	}

	// shifts change receiver
	shift { |addOffset|
		addOffset.isKindOf(Integer).not.if {
			SimpleInitError("Shift argument must be Integer").throw;
		};
		(mode == \points).if {
			list.size.do { |i| list[i] = list[i] + addOffset };
     		^this
		}{
			^this.miSC_setOffset(offset !? (_ + addOffset))
		}
	}

	>> { |addOffset| ^this.shift(addOffset) }


	shiftTo { |targetOffset|
		^(mode == \points).if {
			(list.size != 0).if { this.shift(targetOffset - list[0]) }{ this }
		}{
			offset.isNil.if { this }{ this.shift(targetOffset - offset) }
		}
	}

	>>! { |targetOffset| ^this.shiftTo(targetOffset) }

	shiftToZero { ^list.shiftTo(0) }


	// we want to copy the Sieve's list too
	copy { ^this.deepCopy }

	// new Sieve, same list object
	weakCopy { ^this.superPerform(\copy) }

	// take over mode and offset from Sieve and pass an appropriate array
	copyWith { |seqCollection, withCheck = true|
		var new = this.weakCopy;

		seqCollection.isKindOf(SequenceableCollection).not.if {
			SimpleInitError(
				"copyWith expects SequenceableCollection as first arg"
			).throw
		};

		seqCollection.every(_.isInteger).not.if {
			SimpleInitError(
				"SequenceableCollection must consist of Integers"
			).throw
		};

		(mode == \points).if {
			withCheck.if {
				(seqCollection.every { |point, i|
					(point > 0) and: { (i == 0) or: { point > seqCollection[i-1] } }
				}).not.if {
					SimpleInitError(
						"With mode = \points SequenceableCollection " ++
						"must be strictly ascending"
					).throw
				}
			};
		}{
			withCheck.if {
				seqCollection.every(_>0).not.if {
					SimpleInitError(
						"With mode = \intervals SequenceableCollection" ++
						"must consist of positive Integers"
					).throw
				}
			};
		}
		^new.miSC_setList(seqCollection.asList)
	}

	// apply operator or Function to List and pass the result
	// to a new Sieve with copied mode and offset
	copyApplyTo { |operator, withCheck = true|
		^this.copyWith(operator.applyTo(this.list), withCheck)
	}

	// equality also considers different modes
	== { |that|
		^(this.mode == that.mode).if {
			list == that.list and: { this.offset == that.offset }
		}{
			(this.mode == \points).if {
				this == that.weakCopy.toPoints
			}{
				that == this.weakCopy.toPoints
			}
		}
	}


	// segment methods are most efficient for receiver of mode == \points,
	// output is new Sieve object of mode \points anyway
	segmentGreaterEqual { |lo|
		var index, point, sieve, doCheck = true;
		sieve = Sieve.newEmpty;
		^(mode == \points).if {
			(list.size != 0).if {
				index = list.miSC_lowestIndexForWhichGreaterEqual(lo);
				index.notNil.if { sieve.miSC_setList(list.copyRange(index, list.size-1)) }
			};
			sieve
		}{
			offset.notNil.if {
				point = offset;
				(offset >= lo).if { sieve.list.add(point); doCheck = false };

				list.do { |interval|
					point = point + interval;
					doCheck.if {
						(point >= lo).if { doCheck = false; sieve.list.add(point) }
					}{
						sieve.list.add(point)
					}
				}
			};
			sieve
		}.miSC_setMode(\points);
	}

	segmentLessEqual { |hi|
		var index, point, sieve;
		sieve = Sieve.newEmpty;
		^(mode == \points).if {
			(list.size != 0).if {
				index = list.miSC_highestIndexForWhichLessEqual(hi);
				index.notNil.if { sieve.miSC_setList(list.copyRange(0, index)) }
			};
			sieve
		}{
			offset.notNil.if {
				point = offset;
				index = 0;
				while { index <= (list.size-1) }{
					point = point + list[index];
					(point <= hi).if { sieve.list.add(point) }{ index = list.size - 1 };
					index = index + 1;
				}
			};
			sieve
		}.miSC_setMode(\points);
	}

	// extra method is more efficient than to apply segmentLessEqual and segmentGreaterEqual
	segmentBetweenEqual { |lo, hi|
		var index, point, sieve, doLoCheck = true, loIndex, hiIndex;
		sieve = Sieve.newEmpty;
		^(lo > hi).if {
			sieve
		}{
			(mode == \points).if {
				(list.size != 0).if {
					loIndex = list.miSC_lowestIndexForWhichGreaterEqual(lo);
					hiIndex = list.miSC_highestIndexForWhichLessEqual(hi);
					(loIndex.isNil or: { hiIndex.isNil }).if {
						sieve
					}{
						sieve.miSC_setList(list.copyRange(loIndex, hiIndex))
					}
				}{
					sieve
				}
			}{
				offset.notNil.if {
					point = offset;
					index = 0;
					while { index <= (list.size-1) }{
						point = point + list[index];

						doLoCheck.if {
							(point >= lo).if {
								doLoCheck = false;
								(point <= hi).if {
									sieve.list.add(point)
								}{
									index = list.size - 1
								};
							}
						}{
							(point <= hi).if {
								sieve.list.add(point)
							}{
								index = list.size - 1
							};
						};
						index = index + 1;
					}
				};
				sieve
			}
		}.miSC_setMode(\points);
	}


	segmentGreater { |lo| ^this.segmentGreaterEqual(lo + 1) }

	>=! { |lo| ^this.segmentGreaterEqual(lo) }
	>! { |lo| ^this.segmentGreaterEqual(lo + 1) }


	segmentLess { |hi| ^this.segmentLessEqual(hi - 1) }

	<=! { |hi| ^this.segmentLessEqual(hi) }
	<! { |hi| ^this.segmentLessEqual(hi - 1) }


	segmentBetween { |lo, hi| ^this.segmentBetweenEqual(lo + 1, hi - 1) }

	// expects array [lo, hi]
	<>=! { |bounds| ^this.segmentBetweenEqual(*bounds) }
	<>! { |bounds| ^this.segmentBetweenEqual(bounds[0] + 1, bounds[1] - 1) }



	printOn { arg stream;
		if (stream.atLimit, { ^this });
		stream << "Sieve(" ;
		stream << (this.mode.asString ++ ", ");
		(this.mode == \intervals).if { stream << (this.offset.asString ++ ", ") };
		list.printOn(stream);
		stream << ")" ;
	}

	storeOn { arg stream;
		var thisMode;
		if (stream.atLimit, { ^this });

		stream << "[ " ;
		list.storeItemsOn(stream);
		stream << " ]" ;

		stream << ".toSieve(";
		stream << offset.asString;
		stream << ", \\";
		thisMode = this.mode.asString;
		stream << thisMode;
		stream << ", \\";
		stream << thisMode;
		stream << ")";
	}

	// private

	// These setters don't check the Sieve's state,
	// the user should not directly set mode and offset.

	miSC_setMode { |v| mode = v }
	miSC_setOffset { |v| offset = v }
	miSC_setList { |v| list = v }

	miSC_sieveOp { |args, withOffsets = false, doSum = false, op|
		var goOn = true, last, counts, items, offsets, limit, next,
		minIndices, min, leftIndices, counter, minIndicesDict = IdentityDictionary.new,
		indicesQueue = PriorityQueue.new;

		#items, offsets, limit = (withOffsets.if {
			this.miSC_checkOffsetArgs(args);
		}{
			this.miSC_checkArgs(args);
		});

		offsets.isNil.if { offsets = 0!(items.size) };

		// adjust offset if Sieve is passed
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

		items = items.collect(_.miSC_streamifySieveItems);
		counts = offsets;
		counts.do { |offset, i|
			((offset.notNil) and: { offset <= limit }).if {
				indicesQueue.put(offset, i);
				leftIndices = leftIndices.add(i);
			}
		};

		mode = doSum.if { \points }{ \intervals };

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

			// operator decides what happens at this partial sum
			doSum.if {
				case
				{ op == \u }
				{ list.add(min); last = min }
				{ op == \s }
				{ (minIndices.size == items.size).if { list.add(min); last = min } }
				{ op == \sd }
				{ minIndices.isInteger.if { list.add(min); last = min } }
				{ op == \d }
				{ (minIndices == 0).if { list.add(min); last = min } }
			}{
				case
				{ op == \u }
				{
					last.notNil.if { list.add(min - last) }{ offset = min };
					last = min
				}
				{ op == \s }
				{
					(minIndices.size == items.size).if {
						last.notNil.if { list.add(min - last) }{ offset = min };
						last = min
					}
				}
				{ op == \sd }
				{
					minIndices.isInteger.if {
						last.notNil.if { list.add(min - last) }{ offset = min };
						last = min
					}
				}
				{ op == \d }
				{
					(minIndices == 0).if {
						last.notNil.if { list.add(min - last) }{ offset = min };
						last = min
					}
				}
			};

			// faster than minIndices.asArray.do { ...
			counter = { |i|
				next = items[i].next;
				next.notNil.if {
					counts[i] = counts[i] + next;
					(counts[i] <= limit).if { indicesQueue.put(counts[i], i) }
				};
				(next.isNil or: { counts[i] > limit }).if { leftIndices.remove(i) };
			};
			minIndices.isInteger.if { counter.(minIndices) }{ minIndices.keysDo(counter.(_)) };

			case
				{ op == \u }
				{ (leftIndices.size == 0).if { goOn = false } }
				{ op == \s }
				{ (leftIndices.size < (items.size)).if { goOn = false } }
				{ op == \sd }
				{ (leftIndices.size == 0).if { goOn = false } }
				{ op == \d }
				{ (leftIndices[0] != 0).if { goOn = false } };
		} };

		^this
	}


	miSC_checkOffsetArgs { |args|
		var offsets, items, limit, maxSize = args.size,
		classes = [Integer, Pattern, Stream, Sieve];
		args.size.odd.if {
			limit = args.last.value;
			limit.isKindOf(Integer).not.if {
				SimpleInitError(
					"limit -- given as last item of an odd number " ++
					"of args --\n must be or eveluate to an Integer").throw
			};
			maxSize = maxSize - 1
		}{
			limit = Sieve.limit
		};
		maxSize.do { |i|
			i.even.if {
				items = items.add(args[i])
			}{
				offsets = offsets.add(args[i])
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
		^[items, offsets, limit]
	}


	miSC_checkArgs { |args|
		var items = args, limit,
		classes = [Integer, Pattern, Stream, Sieve];

		args.last.isKindOf(Ref).if {
			items = items.drop(-1);
			// if new gets a Ref eval twice
			limit = args.last.value.value;
			limit.isKindOf(Integer).not.if {
				SimpleInitError("limit -- given as last arg wrapped " ++
					"into a Ref object --\n" ++
					"must be an Integer"
				).throw
			}
		}{
			limit = Sieve.limit
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
		^[items, nil, limit]
	}
}

+SequenceableCollection {

	// iterated bisection, expects ordered list
	miSC_lowestIndexForWhichGreaterEqual { |lo|
		var goOn = true, loIndex = 0, hiIndex = this.size - 1, newIndex, index;

		(this[hiIndex] < lo).if { goOn = false };
		(this[loIndex] >= lo).if { goOn = false; index = 0 };

		while { goOn }{
			((hiIndex - loIndex) <= 1).if { goOn = false; index = hiIndex };
			newIndex = div(hiIndex + loIndex, 2);

			(this[newIndex] == lo).if { goOn = false; index = newIndex };
			(this[newIndex] < lo).if { loIndex = newIndex }{ hiIndex = newIndex };
		};

		^index
	}

	// iterated bisection, expects ordered list
	miSC_highestIndexForWhichLessEqual { |hi|
		var goOn = true, loIndex = 0, hiIndex = this.size - 1, newIndex, index;

		(this[loIndex] > hi).if { goOn = false };
		(this[hiIndex] <= hi).if { goOn = false; index = hiIndex };

		while { goOn }{
			((hiIndex - loIndex) <= 1).if { goOn = false; index = loIndex };
			newIndex = div(hiIndex + loIndex, 2);

			(this[newIndex] == hi).if { goOn = false; index = newIndex };
			(this[newIndex] > hi).if { hiIndex = newIndex }{ loIndex = newIndex };
		};

		^index
	}

}

