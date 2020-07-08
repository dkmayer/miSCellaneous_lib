
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


PL_Pattern : Pattern {
	var <>envir;
	*new { |envir = \current| ^super.new.envir_(envir)  }

	storeArgs { ^[envir] }
	
	data { ^this.envir.miSC_getEnvir[this.item] }
}

PL_ListPattern : PL_Pattern {
	var <>list, <>repeats = inf, <>cutItems;
	*new { |list, repeats = inf, cutItems = true, envir = \current|
		^super.newCopyArgs(envir, list, repeats, cutItems)
	}
	storeArgs { ^[list, repeats, cutItems, envir] }
}


// "stream-save" Pn

PL_Pn : PL_Pattern {
	var <>item, <>repeats;
	*new { |item, repeats = inf, envir = \current|
		^super.newCopyArgs(envir, item, repeats)
	}
	embedInStream { |inval|
		var next;
		item.isKindOf(Stream).if {
			repeats.value(inval).do {
				while {
					next = item.next(inval);
					next.notNil;
				}{
					inval = next.yield;
				};
				item.reset
			};
		}{
			repeats.value(inval).do { inval = item.embedInStream(inval) }
		};
		^inval;
	}

	storeArgs { ^[item, repeats, envir] }
}



// PL_PproxyNth: Pattern for embedding the nth element of a PL_ListPattern

// If nth element is updated just at the time a pattern or item at that position is being embedded,
// then it is replaced ad hoc if flag cutItems equals 1 or true, otherwise it might be embedded at next occasion.


PL_PproxyNth : PL_Pattern {
	var <>listProxy, <>index, <>type, <>doWrap, <>cutItems;
	*new { |listProxy, index, type = 1, doWrap = true, cutItems = true, envir = \current|
		// type: repeats factor for objects other than Patterns and Streams to be embedded
		^super.newCopyArgs(envir, listProxy, index, type, doWrap, cutItems)
	}

	nonListError { |src|
		Error("PL_ListPattern requires list or reference to a " ++
			"non-empty collection; received " ++ src.asString ++ ".").throw;
	}

	embedInStream { |inval|
		var stream, next, src, rep, count = 0;

		listProxy.listHasChanged.not.if {
			src = listProxy.source(index, doWrap);
			rep = src.miSC_repTypeFactor(type);
			stream = PL_Pn(src, rep, envir).asStream;
			while {
				listProxy.update(index);
				// must check next value as we don't want replacement if stream is at end
				next = listProxy.listHasChanged.not.if {
					stream.next(inval)
				};
				// possible replacement of nth item:
				// third condition with count is necessary as otherwise a former replacement
				// wouldn't be detected with cutItems == false
				(listProxy.itemHasChanged && next.notNil && (cutItems.miSC_check || (count == 0))).if {
						src = listProxy.source(index, doWrap);
						rep = src.miSC_repTypeFactor(type);
						stream = PL_Pn(src, rep, envir).asStream;
						next = listProxy.listHasChanged.not.if { stream.next(inval) };
				};

				next.isNil.if { listProxy.itemHasChanged = false; false }{ true };
			}{
				count = count + 1;
				inval = next.yield;
			};
		};
		^inval
	}

	storeArgs { ^[listProxy, index, type, doWrap, cutItems, envir] }
}



PL_Pproxy : PL_Pattern {
	var <>itemProxy, <>type, <>repeats;
	*new { |itemProxy, type = inf, repeats = 1, envir = \current|
		// type = 1 or inf, defines how to embed
		// items other than Patterns or Streams
		^super.newCopyArgs(envir, itemProxy, type, repeats)
	}
	embedInStream { |inval|
		var stream, next, src, repTypeFactor;
			src = itemProxy.source;
			repTypeFactor = src.miSC_repTypeFactor(type);
			stream = PL_Pn(src, repTypeFactor * repeats, envir).asStream;
			while {
				itemProxy.update;
				itemProxy.itemHasChanged.if {
					src = itemProxy.source;
					repTypeFactor = src.miSC_repTypeFactor(type);
					stream = PL_Pn(src, repTypeFactor * repeats, envir).asStream;
					itemProxy.itemHasChanged = false;
				};
				next = stream.next(inval);
				next.isNil.if { ^inval }{ true };
			}{
				inval = next.yield;
			};
		^inval
	}

	storeArgs { ^[itemProxy, type, repeats, envir] }
}


// PL_ includes option for embed type
// type = 1 or inf, defines how to embed
// items other than Patterns or Streams

PL : PL_Pattern {
	var <>item, <>repeats, <>type;
	*new { |item, repeats = inf, type = 1, envir = \current|
		^super.newCopyArgs(envir, item, repeats, type)
	}

	embedInStream { |inval|
		var itemProxy, repeatsProxy, stream, next;
		itemProxy = PL_Proxy(item, envir);
		repeatsProxy = PL_Proxy(repeats, envir);
		stream = PL_Pproxy(itemProxy, type, repeatsProxy.source.value(inval), envir).asStream;
		while {
			next = stream.next(inval);
			next.isNil.if { ^inval }{ true };
		}{
			inval = next.yield;
		};
		^inval
	}

	storeArgs { ^[item, repeats, type, envir] }
}


// The following implementations are based on the corresponding classes in the main library
// Thanks to all authors, especially J.Harkins


PLseq : PL_ListPattern {
	var <>offset;
	*new { |list, repeats = inf, offset = 0, cutItems = true, envir = \current|
		^super.new(list, repeats, cutItems, envir).offset_(offset);
	}
	embedInStream { |inval|
		var item, listProxy, offsetProxy, repeatsProxy, cutItemsProxy, ref = `true;
		listProxy = PL_ListProxy(list, envir).update;
		// avoid an infinite loop case
		listProxy.streamSources.isNil.if { ^inval };

		offsetProxy = PL_Proxy(offset, envir);
		repeatsProxy = PL_Proxy(repeats, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);
		
		(inval.eventAt('reverse') == true).if {
			repeatsProxy.source.value(inval).do { |j|
				listProxy.reverseDo ({ |x,i|
					inval = PL_PproxyNth(listProxy, i + offsetProxy.value, 
						cutItems: cutItemsProxy.value, envir: envir).embedInStream(inval);
				 	listProxy.listHasChanged.if {
					 	ref.value = false;
					 	listProxy.listHasChanged = false;
				 	};
				}, ref.value = true);
			};
		}{
			repeatsProxy.source.value(inval).do { |j|
				 listProxy.do ({ |x, i|
				 	item = PL_PproxyNth(listProxy, i + offsetProxy.value, 
				 		cutItems: cutItemsProxy.value, envir: envir).embedInStream(inval);
				 	inval = item;
				 	listProxy.listHasChanged.if {
					 	ref.value = false;
					 	listProxy.listHasChanged = false;
				 	};
				}, ref.value = true);
			};
		};
		^inval;
	}
	storeArgs { ^[list, repeats, offset, cutItems, envir] }
}



PLser : PLseq {

	embedInStream { |inval|
		var item, listProxy, offsetProxy, repeatsProxy, cutItemsProxy, j = 0;

		listProxy = PL_ListProxy(list, envir).update;
		offsetProxy = PL_Proxy(offset, envir);
		repeatsProxy = PL_Proxy(repeats, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

		(inval.eventAt('reverse') == true).if {
			listProxy.reverseDoN ({ |x,i|
				inval = PL_PproxyNth(listProxy, i - j + offsetProxy.value, 
					cutItems: cutItemsProxy.value, envir: envir)
					.embedInStream(inval);
				listProxy.listHasChanged.if {
					listProxy.listHasChanged = false;
					j = i;
				};
			}, repeatsProxy.source.value(inval));
		}{
			listProxy.doN ({ |x,i|
				inval = PL_PproxyNth(listProxy, i - j + offsetProxy.value, 
					cutItems: cutItemsProxy.value, envir: envir)
					.embedInStream(inval);
				listProxy.listHasChanged.if {
					listProxy.listHasChanged = false;
					j = i;
				};
			}, repeatsProxy.source.value(inval));
		};
		^inval;
	}
}


PLshuf : PL_ListPattern {
	*new { |list, repeats = inf, cutItems = true, envir = \current|
		^super.new(list, repeats, cutItems, envir)
	}
	embedInStream { |inval|
		var item, stream, order, listProxy, repeatsProxy, cutItemsProxy;
		listProxy = PL_ListProxy(list, envir).update;
		repeatsProxy = PL_Proxy(repeats, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

		order = (0..(listProxy.listSource.size-1)).scramble;

		repeatsProxy.source.value(inval).do { |j|
			listProxy.listHasChanged.if {
				order = (0..(listProxy.listSource.size-1)).scramble;
				listProxy.listHasChanged = false;
			};
			order.do { |i|
				inval = PL_PproxyNth(listProxy, i, 
				 	cutItems: cutItemsProxy.value, envir: envir).embedInStream(inval);
			};
		};
		^inval;
	}

	storeArgs { ^[list, repeats, cutItems, envir] }
}


PLshufn : PLshuf {

	embedInStream { |inval|
		var item, stream, order, listProxy, repeatsProxy, cutItemsProxy;

		listProxy = PL_ListProxy(list, envir).update;
		repeatsProxy = PL_Proxy(repeats, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

		repeatsProxy.source.value(inval).do { |j|
			order = (0..(listProxy.listSource.size-1)).scramble;
			order.do { |i|
				inval = PL_PproxyNth(listProxy, i, 
				 	cutItems: cutItemsProxy.value, envir: envir).embedInStream(inval);
				listProxy.listHasChanged = false;
			};
		};
		^inval;
	}
}

PLrand : PL_ListPattern {
	*new { |list, repeats = inf, cutItems = true, envir = \current|
		^super.new(list, repeats, cutItems, envir)
	}

	embedInStream { |inval|
		var index, listProxy, repeatsProxy, cutItemsProxy;

		listProxy = PL_ListProxy(list, envir).update;
		repeatsProxy = PL_Proxy(repeats, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

		repeatsProxy.source.value(inval).do { |i|
			index = listProxy.size.rand;
			inval = PL_PproxyNth(listProxy, index, 
				cutItems: cutItemsProxy.value, envir: envir).embedInStream(inval);
			listProxy.listHasChanged = false;
		};
		^inval;
	}
}


PLxrand : PL_ListPattern {
	*new { |list, repeats = inf, cutItems = true, envir = \current|
		^super.new(list, repeats, cutItems, envir)
	}

	embedInStream { |inval|
		var item, size, index, listProxy, repeatsProxy, cutItemsProxy;

		listProxy = PL_ListProxy(list, envir).update;
		repeatsProxy = PL_Proxy(repeats, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

		index = listProxy.size.rand;
		repeatsProxy.source.value(inval).do { |i|
			size = listProxy.size;
			index = (index + (size - 1).rand + 1) % size;
			inval = PL_PproxyNth(listProxy, index, 
				cutItems: cutItemsProxy.value, envir: envir).embedInStream(inval);
			listProxy.listHasChanged = false;
		};
		^inval;
	}
}


PLwrand : PL_ListPattern {
	var <>weights;
	*new { |list, weights, repeats = inf, cutItems = true, envir = \current|
		^super.new(list, repeats, cutItems, envir).weights_(weights)
	}

	embedInStream { |inval|
		var item, weightsStream, nextWeight, listProxy, weightsProxy, repeatsProxy, cutItemsProxy;

		listProxy = PL_ListProxy(list, envir).update;
		repeatsProxy = PL_Proxy(repeats, envir);
		weightsProxy = PL_Proxy(weights, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

		weightsStream = PL_Pproxy(weightsProxy, envir: envir).asStream;

		repeatsProxy.source.value(inval).do { |i|
			nextWeight = weightsStream.next;
			nextWeight.isNil.if { ^inval };
			inval = PL_PproxyNth(listProxy, nextWeight.windex, 
				cutItems: cutItemsProxy.value, envir: envir).embedInStream(inval);
			listProxy.listHasChanged = false;
		};
		^inval
	}

	storeArgs { ^[list, weights, repeats, cutItems, envir] }
}

PLswitch : PL_Pattern {
	var <>list, <>which = 0, <>cutItems;
	*new { |list, which = 0, cutItems = true, envir = \current|
		^super.newCopyArgs(envir, list, which, cutItems)
	}
	embedInStream { |inval|
		var item, index, listProxy, indexProxy, indexStream, cutItemsProxy;
		listProxy = PL_ListProxy(list, envir).update;
		indexProxy = PL_Proxy(which, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

		indexStream = PL_Pproxy(indexProxy, envir: envir).asStream;

		while {
			(index = indexStream.next(inval)).notNil;
		}{
			inval = PL_PproxyNth(listProxy, index, 
				cutItems: cutItemsProxy.value, envir: envir).embedInStream(inval);
			listProxy.listHasChanged = false;
		};
		^inval;
	}
	storeArgs { ^[list, which, cutItems, envir] }
}


PLswitch1 : PLswitch {

	embedInStream { |inval|
		var index, outval, indexProxy, listProxy, streamList, indexStream, cutItemsProxy;

		listProxy = PL_ListProxy(list, envir).update;
		indexProxy = PL_Proxy(which, envir);
		indexStream = PL_Pproxy(indexProxy, envir: envir).asStream;
		cutItemsProxy = PL_Proxy(cutItems, envir);

		streamList = listProxy.size.collect { |i|
			PL_PproxyNth(listProxy, i, inf, cutItems: cutItems, envir: envir).asStream
		};

		loop {
			listProxy.update.listHasChanged.if {
				streamList = listProxy.size.collect { |i|
					PL_PproxyNth(listProxy, i, inf, 
						cutItems: cutItemsProxy.value, envir: envir).asStream
				};
				listProxy.listHasChanged = false;
			};
			(index = indexStream.next).isNil.if { ^inval };
			outval = streamList[index].next(inval);
			outval.isNil.if { ^inval };
			inval = outval.yield;
		};
	}
}


PLtuple : PL_ListPattern {

	embedInStream { |inval|
		var item, streamList, tuple, outval, listProxy, repeatsProxy, cutItemsProxy;
		listProxy = PL_ListProxy(list, envir).update;
		repeatsProxy = PL_Proxy(repeats, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

		repeatsProxy.source.value(inval).do { |j|
			var sawNil = false;
			streamList = listProxy.size.collect { |i|
				PL_PproxyNth(listProxy, i, inf, 
					cutItems: cutItemsProxy.value, envir: envir).asStream
			};

			while {
				listProxy.update.listHasChanged.if {
					streamList = listProxy.size.collect { |i|
						PL_PproxyNth(listProxy, i, inf, 
							cutItems: cutItemsProxy.value, envir: envir).asStream
					};
					listProxy.listHasChanged = false;
				};
				tuple = Array.new(streamList.size);
				streamList.do { |stream, j|
					outval = stream.next(inval);
					outval.isNil.if { sawNil = true; };
					tuple.add(outval);
				};
				sawNil.not
			}{
				inval = yield(tuple);
			};
		};
		^inval;
	}
}


PLslide : PL_ListPattern {
	var <>len, <>step, <>start, <>wrapAtEnd;
	*new { |list, repeats = inf, len = 3, step = 1, start = 0, wrapAtEnd = true, 
		cutItems = true, envir = \current|
		^super.new(list, repeats).len_(len).step_(step)
		.start_(start).wrapAtEnd_(wrapAtEnd).cutItems_(cutItems).envir_(envir);
	}
	embedInStream { |inval|
		var item, pos, stepStream, stepVal, lengthStream, lengthVal, next, stream,
   	 		listProxy, repeatsProxy, lengthProxy, stepProxy, 
   	 		startProxy, wrapAtEndProxy, cutItemsProxy;

   	 	listProxy = PL_ListProxy(list, envir).update;
		repeatsProxy = PL_Proxy(repeats, envir);
		lengthProxy = PL_Proxy(len, envir);
		stepProxy = PL_Proxy(step, envir);
		startProxy = PL_Proxy(start, envir);
		wrapAtEndProxy = PL_Proxy(wrapAtEnd, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

  	  	stepStream = PL_Pproxy(stepProxy, envir: envir).asStream;
  	  	lengthStream = PL_Pproxy(lengthProxy, envir: envir).asStream;
		pos = startProxy.source.value;

		repeatsProxy.source.value(inval).do {
			lengthVal = lengthStream.next(inval);
	 	  	lengthVal.isNil.if { ^inval };
			wrapAtEndProxy.source.value.if {
				lengthVal.do { |j|
					item = PL_PproxyNth(listProxy, pos + j, 
						cutItems: cutItemsProxy.value, envir: envir);
					inval = item.embedInStream(inval);
					listProxy.listHasChanged = false;
				}
			}{
				lengthVal.do { |j|
					listProxy.source(pos + j, false).isNil;
					stream = PL_PproxyNth(listProxy, pos + j, doWrap: false, 
						cutItems: cutItemsProxy.value, envir: envir).asStream;
					while {
						next = stream.next;
						listProxy.source(pos + j, false).isNil.if { ^inval };
						next.notNil
					}{
						inval = next.embedInStream(inval);
					}
				};
			};
    			stepVal = stepStream.next(inval);
    			stepVal.isNil.if { ^inval };
    			pos = pos + stepVal;
		};
		^inval;
    }

	storeArgs { ^[list, repeats, len, step, start, wrapAtEnd, cutItems, envir] }
}


PLwalk : PL_ListPattern {

	var	<>start, <>step, <>direction;
	*new { |list, step, direction = 1, start = 0, cutItems = true, envir = \current|
		^super.new(list).start_(start)
			.step_(step ?? { Prand([-1, 1], inf) })
			.direction_(direction).cutItems_(cutItems).envir_(envir);
	}

	embedInStream { |inval|
		var	step, index, stepStream, directionStream, direction,
			listProxy, stepProxy, directionProxy, startProxy, cutItemsProxy;

    		listProxy = PL_ListProxy(list, envir).update;
		stepProxy = PL_Proxy(this.step, envir);
		directionProxy = PL_Proxy(this.direction, envir);
		startProxy = PL_Proxy(start, envir);
		cutItemsProxy = PL_Proxy(cutItems, envir);

   	 	stepStream = PL_Pproxy(stepProxy, envir: envir).asStream;
   	 	directionStream = PL_Pproxy(directionProxy, envir: envir).asStream;

		index = startProxy.source.value;
		direction = directionStream.next(inval) ? 1;

		while {
			step = stepStream.next(inval);
			step.notNil
		}{
			inval = PL_PproxyNth(listProxy, index, 
				cutItems: cutItemsProxy.value, envir: envir).embedInStream(inval);
			listProxy.listHasChanged = false;

			step = step * direction;
			(((index + step) < 0) or: { (index + step) >= listProxy.size }).if {
				direction = directionStream.next(inval) ? 1;
				step = step.abs * direction.sign;
			};
			index = (index + step) % listProxy.size;
		};
		^inval;
	}

	storeArgs { ^[list, step, direction, start, cutItems, envir] }
}

//////////////////////

PLwhite : Pwhite {
	var <>envir;
	*new { |lo = 0.0, hi = 1.0, length = inf, envir = \current|
		^super.new(PL(lo, 1, inf, envir), PL(hi, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[lo, hi, length, envir] }
}


PLlprand : Plprand {
	var <>envir;
	*new { |lo = 0.0, hi = 1.0, length = inf, envir = \current|
		^super.new(PL(lo, 1, inf, envir), PL(hi, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[lo, hi, length, envir] }
}


PLhprand : Phprand {
	var <>envir;
	*new { |lo = 0.0, hi = 1.0, length = inf, envir = \current|
		^super.new(PL(lo, 1, inf, envir), PL(hi, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[lo, hi, length, envir] }
}

PLmeanrand : Pmeanrand {
	var <>envir;
	*new { |lo = 0.0, hi = 1.0, length = inf, envir = \current|
		^super.new(PL(lo, 1, inf, envir), PL(hi, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[lo, hi, length, envir] }
}


PLbrown : Pbrown {
	var <>envir;
	*new { |lo = 0.0, hi = 1.0, step = 0.125, length = inf, envir = \current|
		^super.new(PL(lo, 1, inf, envir), PL(hi, 1, inf, envir), PL(step, 1, inf, envir),
			PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[lo, hi, step, length, envir] }
}


PLgbrown : Pgbrown {
	var <>envir;
	*new { |lo = 0.0, hi = 1.0, step = 0.125, length = inf, envir = \current|
		^super.new(PL(lo, 1, inf, envir), PL(hi, 1, inf, envir), PL(step, 1, inf, envir),
			PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[lo, hi, step, length, envir] }
}

//////////////////////

PLseries : Pseries {
	var <>envir;
	*new { |start = 0, step = 1, length = inf, envir = \current|
		^super.new(PL(start, 1, inf, envir).asStream, PL(step, 1, inf, envir),
			PL(length, length, inf, envir).asStream).envir_(envir)
	}
	storeArgs { ^[start, step, length, envir] }
}


PLgeom : Pgeom {
	var <>envir;
	*new { |start = 0, grow = 1, length = inf, envir = \current|
		^super.new(PL(start, 1, inf, envir).asStream, PL(grow, 1, inf, envir),
			PL(length, length, inf, envir).asStream).envir_(envir)
	}
	storeArgs { ^[start, grow, length, envir] }
}

//////////////////////


PLbeta : Pbeta {
	var <>envir;
	*new { |lo = 0.0, hi = 1.0, prob1 = 1, prob2 = 1, length = inf, envir = \current|
		^super.new(PL(lo, 1, inf, envir), PL(hi, 1, inf, envir), PL(prob1, 1, inf, envir),
			PL(prob2, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[lo, hi, prob1, prob2, length, envir] }
}

PLcauchy : Pcauchy {
	var <>envir;
	*new { |mean = 0.0, spread = 1.0, length = inf, envir = \current|
		^super.new(PL(mean, 1, inf, envir), PL(spread, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[mean, spread, length, envir] }
}

PLgauss : Pgauss {
	var <>envir;
	*new { |mean = 0.0, dev = 1, length = inf, envir = \current|
		^super.new(PL(mean, 1, inf, envir), PL(dev, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[mean, dev, length, envir] }
}


PLpoisson : Ppoisson {
	var <>envir;
	*new { |mean = 1, length = inf, envir = \current|
		^super.new(PL(mean, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[mean, length, envir] }
}

PLexprand : Pexprand {
	var <>envir;
	*new { |lo = 0.0001, hi = 1.0, length = inf, envir = \current|
		^super.new(PL(lo, 1, inf, envir), PL(hi, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[lo, hi, length, envir] }
}

//////////////////////

PLnaryop : Pnaryop {
	var <>envir;
	*new { |operator, pat, arglist, envir = \current|
		^super.new(operator, PL(pat, 1, inf, envir), arglist.collect(PL(_, 1, inf, envir)) ).envir_(envir)
	}
}

PLnaryFunc : Pnaryop {
	var <>envir;
	*new { |func, pat, arglist, envir = \current|
		^super.new(\miSC_applyNaryFuncProxy,
			PL(pat, 1, inf, envir), [PL(func, 1, inf, envir)] ++ arglist.collect(PL(_, 1, inf, envir)) ).envir_(envir)
	}
}


//////////////////////

PLn : Pn {
	var <>item, <>repeats, <>envir;
	*new { |item, repeats = inf, envir = \current|
		^super.newCopyArgs(
			Plazy({ envir.miSC_getEnvir[item] }),
			PL(repeats, repeats, 1, envir)
		)
	}
	storeArgs { ^[item, repeats, envir] }
}


		
