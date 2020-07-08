
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


Pshufn : ListPattern {
	embedInStream { |inval|
		var item, stream;
		repeats.value(inval).do { |j|
			(0..(list.size-1)).scramble.do { |i|
				item = list.wrapAt(i);
				inval = item.embedInStream(inval);
			};
		};
		^inval;
	}
}

PlaceAll : Ppatlace {
	*new { |list, repeats = 1, offset = 0|
		var laceList = list.miSC_makeLaceList;
		^super.new(laceList, repeats, offset);

	}
}


//////////////////


PmonoPar : Plazy {
	*new { |setPatternPairs, defname = \default, offset = 1e-6|
		^super.new({
			var g = Group(), cleanup = EventStreamCleanup.new;
			Pseq([
				Pbind(
					\instrument, defname,
					\type, Pseq([\on]),
					\dur, offset,
					\group, g,
					\cleanup, Pfunc { |e| cleanup.addFunction(e, { g.release }) }
				),
				Ptpar(setPatternPairs.collect { |p, j|
					var pairs, keys = p.select { |x,i| i.even }, keysAfterDur;
					keysAfterDur = keys.copyToEnd(keys.indexOf(\dur) + 1);
					#[note, degree, midinote].any { |x| keysAfterDur.includes(x) }.if {
						keysAfterDur = keysAfterDur.add(\freq)
					};
					pairs = [\type, \set] ++ p ++
						keys.includes(\args).not.if { [\args, keysAfterDur] } ++
						[\id, g];
					[j * offset, Pbind(*pairs)]
				}.flatten),
				Pbind(\instrument, defname, \type, Pseq([\off]), \dur, offset, \group, g)
			])
		})
	}
}

PpolyPar : Plazy {
	*new { |setPatternPairs, defNames = #[default], order, offset = 1e-6|
		^super.new({
			var cleanup, size = defNames.size, groups, hasSynthsForAllStreams, hasNoSynthsForAnyStream;

			cleanup = EventStreamCleanup.new;

			order = order ?? { (0..size-1) };
			groups = nil ! size;
			size.do { |i| groups[order[i]] = (i != 0).if { Group(groups[order[i-1]], \addAfter) }{ Group() } };

			hasSynthsForAllStreams = setPatternPairs.every { |x| x.includes(\synths) };
			hasNoSynthsForAnyStream = setPatternPairs.every { |x| x.includes(\synths).not };

			(hasSynthsForAllStreams.not && (hasNoSynthsForAnyStream && (size == setPatternPairs.size)).not).if {
				SimpleInitError("\\synths must be either defined for all streams or the number of streams must " ++
					"equal the number of defNames.\n" ++ "In that case each setting stream refers to the " ++
					"synth at the same position of defNames."
				).throw
			};

			Pseq(
				groups.collect { |g, i|
					Pbind(
						\instrument, defNames[i],
						\type, Pseq([\on]),
						\dur, offset,
						\group, g,
						\cleanup, Pfunc { |e| cleanup.addFunction(e, { g.release }) }
					)
				} ++

				Ptpar(setPatternPairs.collect { |p, j|
					var pairs, keys = p.select { |x,i| i.even }, keysAfterDur;
					keysAfterDur = keys.copyToEnd(keys.indexOf(\dur) + 1);
					#[note, degree, midinote].any { |x| keysAfterDur.includes(x) }.if {
						keysAfterDur = keysAfterDur.add(\freq)
					};
					pairs = hasNoSynthsForAnyStream.if { [\synths, j] }{ [] } ++
						[\type, \set] ++ p ++
						keys.includes(\args).not.if { [\args, keysAfterDur] } ++
						[\id, Pfunc { |e| groups[e[\synths].asArray] } ];
					[j * offset, Pbind(*pairs)]
				}.flatten) ++

				groups.collect { |g, i|
					Pbind(\instrument, defNames[i], \type, Pseq([\off]), \dur, offset, \id, g)
				}
			)
		})
	}
}


//////////////////


PSloop : Pattern {
	var <>psPat, <>lookBack, <>doSkip, <>loopFunc, <>loopFuncPerItem, <>loopFuncAsGoFunc, <>goFunc, <>indices;
	*new { |srcPat, length = inf, bufSize = 1, lookBack = 0, doSkip = 0,
		loopFunc, loopFuncPerItem = 0, loopFuncAsGoFunc = 0, goFunc, indices, copyItems = 0, copySets = 1|
		^super.new.psPat_(PStream(srcPat, length, bufSize, copyItems, copySets))
			.lookBack_(lookBack).doSkip_(doSkip).loopFunc_(loopFunc)
			.loopFuncPerItem_(loopFuncPerItem).loopFuncAsGoFunc_(loopFuncAsGoFunc)
			.goFunc_(goFunc).indices_(indices);
	}
	embedInStream { |inval|
		var psStream = psPat.asStream;
		var lookBackStream, lastVals, lookBackVal, oldLookBackVal, lookBackValClipped, loopLength,
			doSkipVal, count, mappedIndex, indicesStream, rawVal, mappedVal,
			currentIndices, currentIndexOrder, getCurrentIndexOrder, loopFuncStream, currentLoopFunc,
			loopFuncPerItemVal, goFuncStream, currentGoFunc, loopFuncAsGoFuncVal;

		getCurrentIndexOrder = { |nextIndices, indexbase|
			nextIndices.isKindOf(Function).if { nextIndices.(indexbase) }{ nextIndices }
		};

		lookBackStream = case
			{ lookBack.isKindOf(Function) }{ Pfunc(lookBack) }
			{ lookBack.isKindOf(Pattern) }{ lookBack }.asStream;

		indicesStream = case
			{ indices.isKindOf(Function) || indices.isKindOf(SequenceableCollection) }{ Pn(indices) }
			{ indices.isKindOf(Pattern) }{ indices }.asStream;

		loopFuncStream = case
			{ loopFunc.isKindOf(Function) }{ Pn(loopFunc) }
			{ loopFunc.isKindOf(Pattern) }{ loopFunc }.asStream;

		goFuncStream = case
			{ goFunc.isKindOf(Function) }{ Pn(goFunc) }
			{ goFunc.isKindOf(Pattern) }{ goFunc }.asStream;

		lookBackVal = lookBackStream.next;

		while {
			psPat.memoRoutine.notNil.if { lastVals = psPat.lastValues };
			lookBackVal.notNil
		}{
			(lookBackVal > 0).if {
				count = 0;
				lookBackValClipped = min(lookBackVal, psPat.bufSize - 1);
				currentIndices = indicesStream.next;
				currentIndexOrder = getCurrentIndexOrder.(currentIndices, lookBackValClipped);
				currentLoopFunc = loopFuncStream.next;

				while { lookBackVal > 0 }{
					lookBackValClipped = min(lookBackVal, psPat.bufSize - 1);
					currentIndices.notNil.if {
						mappedIndex = currentIndexOrder[count].clip(0, lookBackValClipped - 1);
						loopLength = currentIndexOrder.size;
					}{
						mappedIndex = count.mod(lookBackValClipped);
						loopLength = lookBackValClipped;
					};
					// index has to be reversed as PS buffers last at first
					rawVal = lastVals.at(lookBackValClipped - 1 - mappedIndex);
					mappedVal = currentLoopFunc.notNil.if { currentLoopFunc.(rawVal) }{ rawVal };

					inval = mappedVal.embedInStream(inval);
					count = count + 1;
					loopFuncPerItemVal = loopFuncPerItem.();
					(loopFuncPerItemVal.miSC_check || (count >= loopLength)).if {
						currentLoopFunc = loopFuncStream.next
					};
					doSkipVal = doSkip.();
					(doSkipVal.miSC_check || (count >= loopLength)).if {
						oldLookBackVal = lookBackValClipped;
						lookBackVal = lookBackStream.next;
						lookBackValClipped = min(lookBackVal, psPat.bufSize - 1);
						((oldLookBackVal != lookBackValClipped) || (count >= loopLength)).if {
							count = 0;
							(lookBackVal != 0).if {
								currentIndices = indicesStream.next;
								currentIndexOrder = getCurrentIndexOrder.(currentIndices, lookBackValClipped);
							}
						}
					}
				}
			}{
				loopFuncAsGoFuncVal = loopFuncAsGoFunc.();
				currentGoFunc = loopFuncAsGoFuncVal.miSC_check.if { loopFuncStream }{ goFuncStream }.next;

				rawVal = psStream.next(inval);
				mappedVal = currentGoFunc.notNil.if { currentGoFunc.(rawVal) }{ rawVal };

				inval = mappedVal.embedInStream(inval);
				lookBackVal = lookBackStream.next;
			}
		};
		^inval;
	}
}


//////////////////



// This adapts an idea of James Harkins' PnNilSafe (ddwPatterns quark) for Psym,
// If all patterns in the Dictinonary return nil Psym's embedInStream
// can produce an infinite loop.
// It doesn't yield, so wrapping Psym into a PnNilSafe doesn't help in that case.
// Instead a check with logical time can be built into Psym itself.

PsymNilSafe : Psym {

	var	<>maxNull;

	*new { arg pattern, dict, maxNull = 128;
		^super.new(pattern).dict_(dict ?? { currentEnvironment }).maxNull_(maxNull)
	}

	embedInStream { |inval|
		var str, outval, pat, counter = 0, saveLogicalTime;
		str = pattern.asStream;
		while {
			outval = str.next(inval);
			outval.notNil
		} {
			pat = this.getPattern(outval);
			saveLogicalTime = thisThread.clock.beats;
			inval = pat.embedInStream(inval);

			if(thisThread.clock.beats == saveLogicalTime) {
				counter = counter + 1;
				if(counter > maxNull) { ^inval };
			} {
				counter = 0;
			};

		};
		^inval
	}
}

