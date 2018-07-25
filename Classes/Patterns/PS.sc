
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


// It might look odd that copying is triggered twice in PSx patterns, but -
// especially important with event patterns: copying within the Pfunc function ensures
// that buffered items of the *source* are not altered and second copying
// (delegated to the MemoRoutine) ensures that items buffered in the PSx are not
// altered. It would of be possible to differentiate by defining two further
// copy flags.


PStream : Plazy {
	var <>srcPat, <>length, <>lengthStream, <>bufSize, <>copyItems, <>copySets, <>memoRoutine;

	*new { |srcPat, length = inf, bufSize = 1, copyItems = 0, copySets = 1|
		^super.new.srcPat_(srcPat).length_(length).bufSize_(bufSize)
			.copyItems_(copyItems).copySets_(copySets)
	}
	storeArgs { ^[srcPat, length, bufSize, copyItems, copySets] }

	embedInStream { | inval|
		memoRoutine.isNil.if {
			lengthStream = length.asStream;
			// the MemoRoutine shouldn't be instantiated before streamification (allows dynamic scoping)
			// new streams should refer to the same MemoRoutine
			memoRoutine = MemoRoutine(
				{ |inval| srcPat.embedInStream(inval) },
				bufSize: bufSize,
				copyItems: copyItems,
				copySets: copySets
			)
		};
		func = { |inval|
			Pfinval(lengthStream.next(inval), Pfunc { memoRoutine.next(inval).miSC_copy(copyItems, copySets) })
		};
		^func.value(inval).embedInStream(inval)
	}

	at  { |i| ^memoRoutine.lastValues[i] }

	lastValues  { ^memoRoutine.lastValues }

	lastValue  { ^memoRoutine.lastValues[0] }

	count { ^memoRoutine.count }

	bufSeq  { |dropNils = true|
		var seq = memoRoutine.lastValues.reverse;
		^dropNils.if { seq.reject(_.isNil) }{ seq }
	}

}


PS : PStream {}

Pstream : PStream {}

PSdup : PStream {
	var <>psxPat;

	*new { |psxPat, length = inf, bufSize = 1, copyItems = 0, copySets = 1|
		^super.new(
			psxPat.isKindOf(PStream).not.if { SimpleInitError("PSdup requires PSx source pattern").throw };
			Pfunc { psxPat.lastValue.miSC_copy(copyItems, copySets) }, length, bufSize, copyItems, copySets
		).psxPat_(psxPat);
	}
	storeArgs { ^[psxPat, length, bufSize, copyItems, copySets] }
}


PSrecur : PStream {
	var <>start, <>recurFunc, <>recurFuncStream;
	*new { |recurFunc, length = inf, bufSize, start, copyItems = 0, copySets = 1|

		// adapt bufSize if not given
		bufSize = bufSize ?? {
			(start.isNil || (start.isKindOf(SequenceableCollection).not)).if { 1 }{ start.size };
		};
		// make start an array
		(start.notNil && (start.isKindOf(SequenceableCollection).not)).if { start = [start] };
		^super.new(nil, length, bufSize, copyItems, copySets).start_(start).recurFunc_(recurFunc)
	}
	storeArgs { ^[recurFunc, length, bufSize, start, copyItems, copySets] }

	embedInStream { | inval|
		var replaceSize;
		// the MemoRoutine shouldn't be instantiated before streamification (allows dynamic scoping)
		// new streams should refer to the same MemoRoutine
		memoRoutine.isNil.if {
			lengthStream = length.asStream;
			recurFuncStream = recurFunc.asStream;

			memoRoutine = MemoRoutine({ |inval|
					Pfunc {
						recurFuncStream.next.(memoRoutine.lastValues, memoRoutine.count).miSC_copy(copyItems, copySets)
					}.embedInStream(inval)
				},
				bufSize: bufSize,
				copyItems: copyItems,
				copySets: copySets
			);
			start.notNil.if {
				replaceSize = min(start.size, memoRoutine.lastValues.size);
			 	replaceSize.do { |i| memoRoutine.lastValues[i] = start[start.size - 1 - i] };
			};
		};

		func = { |inval|
			Pfinval(lengthStream.next(inval), Pfunc { memoRoutine.next(inval) })
		};
		^func.value(inval).embedInStream(inval)
	}

}




