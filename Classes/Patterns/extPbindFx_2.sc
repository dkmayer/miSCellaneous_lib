
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


+Symbol {

	miSC_checkPbindFxBusMatch { |sym, firstIsSource = false, secondIsLast = false,
		firstOtherBusArgs, secondOtherBusArgs|

		var firstOutputs = SynthDescLib.global[this].outputs;
		var secondInputs = SynthDescLib.global[sym].inputs;
		var secondOutputs = SynthDescLib.global[sym].outputs;
		var firstInputs = SynthDescLib.global[this].inputs;

		var areFirstInputsOk, areFirstOutputsOk, areSecondInputsOk, areSecondOutputsOk,
			firstOutNumberOfChannels, secondInNumberOfChannels, connIo, ioRef = `nil;

		var inSelector = { |io| io.type != LocalIn };
		var outSelector = { |io| io.type != LocalOut };

		// crucial check for inputs/outputs:
		// counting number of ioTypes (\in or \out), must be 1
		// looking if otherBusArgs allow further ins and outs
		// ioRef gets the relevant connection between fxs in second call of ioCheck

		var ioCheck = { |inOutputs, ioType, ioRef, otherArgs|
			var count = 0;
			inOutputs.every { |io|
				(io.startingChannel == ioType).if { ioRef.value = io; count = count + 1 };
				(io.startingChannel == ioType) or: {
					otherArgs.isNil.if { false } { otherArgs.includes(io.startingChannel) };
				};
			} and: { count == 1 }
		};

		firstInputs = firstInputs.select(inSelector);
		firstOutputs = firstOutputs.select(outSelector);
		secondInputs = secondInputs.select(inSelector);
		secondOutputs = secondOutputs.select(outSelector);

		areFirstInputsOk = firstIsSource.if {
			firstOtherBusArgs.isNil.if {
				firstInputs.size == 0
			}{
				firstInputs.every { |io| firstOtherBusArgs.includes(io.startingChannel) }
			}
		}{
			// if first node (received symbol) denotes effect,
			//its inputs are checked within other match
			true
		};

		areFirstOutputsOk = ioCheck.(firstOutputs, \out, ioRef, firstOtherBusArgs);
		areSecondInputsOk = ioCheck.(secondInputs, \in, ioRef, secondOtherBusArgs);

		connIo = ioRef.value;

		// now connIo is only relevant if all 4 checks are ok

		areSecondOutputsOk = secondIsLast.if {
			ioCheck.(secondOutputs, \out, ioRef, secondOtherBusArgs)
		}{
			// if second node denotes effect before last effect,
			// its outputs are checked within other match
			true
		};

		((areFirstInputsOk && areFirstOutputsOk && areSecondInputsOk && areSecondOutputsOk) and: {
			(firstOutputs[0].numberOfChannels) <= (connIo.numberOfChannels)
		}).not.if {
			SimpleInitError("PbindFx bus mismatch, source and fx synths must match " ++
				"in/out conventions: \n\n" ++
				".) \tFor source and fx SynthDefs there must be only one out ugen " ++
				"using 'out' as bus arg, \n" ++
				"\tLocalOut is allowed, other out ugens can be admitted by \\otherBusArgs. \n" ++
				".) \tSource SynthDefs must not have in ugens, except LocalIn or \n" ++
				"\tthey are admitted by \\otherBusArgs.  \n" ++
				".) \tFx synths must read from buses with ugen In.ar(in, ...) " ++
				"refering to the bus arg 'in', \n\tthere must not be other in ugens " ++
				"within the fx SynthDef, \n\texcept LocalIn or they are admitted by \\otherBusArgs.\n" ++
				".) \tNumber of out channels of preceeding source / fx synth" ++
				" must not be greater than \n" ++
				"\tthe number of the following fx synth's in channels,\n\tthis is checked for " ++
				"all connections of an fx graph."
			).throw;
		};
		^connIo.first.numberOfChannels
	}
}


+SequenceableCollection {

	// order as receiver
	miSC_getFxBusData { |predecessors, successors, fxOrderPositions, fxEvents, srcInstrument,
		otherBusArgs, cleanupDelay, server|
		var cleanupDelaySums = [0], fxBuses, splitBuses,
			splitFactors, fxNum = this.size;

		// 0 (src) can have a split, but last fx in topo order cannot
		splitBuses = {} ! fxNum;
		splitFactors = {} ! fxNum;

		splitFactors[0] = successors[0].asArray.size;
		(splitFactors[0] > 1).if {
			splitBuses[0] = Bus.audio(
				server,
				SynthDescLib.global[srcInstrument].outputs.first.numberOfChannels
			)
		};

		this.do { |fxIndex, i|
			var inSize, inst, prevInst, prevInsts, fxEvent = fxEvents[fxIndex-1], maxPrevCleanupDelaySum = 0;
			inst = (fxEvents[fxIndex-1][\fx]).asSymbol;

			prevInsts = predecessors[fxIndex].asArray.collect { |j|
				// to get the new cleanupDelaySum, we have to add to the
				// max cleanupDelaySum of the predecessors

				// all cleanupDelaySums are calculated without source cleanupDelay first,
				// which is added afterwards, that way branches with no ins have proper cleanupDelay also (think)
				var k = fxOrderPositions[j];

				(cleanupDelaySums[k] >= maxPrevCleanupDelaySum).if {
					maxPrevCleanupDelaySum = cleanupDelaySums[k]
				};

				(j == 0).if { srcInstrument }{ (fxEvents[j - 1][\fx]).asSymbol }
			};

			prevInsts.do { |prevInst|
				inSize = prevInst.miSC_checkPbindFxBusMatch(
					inst,
					i == 0,
					i+1 == fxNum,
					(i == 0).if { otherBusArgs }{ fxEvents[this[i-1]-1][\otherBusArgs] },
					fxEvents[fxIndex-1][\otherBusArgs]
				)
			};

			// is nil if there is no in
			fxBuses = fxBuses.add(inSize.notNil.if { Bus.audio(server, inSize) });

			cleanupDelaySums = cleanupDelaySums.add(
				fxEvent[\cleanupDelay] + maxPrevCleanupDelaySum
			);

			(i < (this.size-1)).if {
				splitFactors[i+1] = successors[fxIndex].asArray.size;
				(splitFactors[i+1] > 1).if {
					splitBuses[i+1] = Bus.audio(
						server,
						SynthDescLib.global[inst].outputs.first.numberOfChannels;
					)
				}
			}
		};
		// adjust cleanupDelaySums to source (see comments above)
		cleanupDelaySums = cleanupDelaySums + cleanupDelay;
		^[fxBuses, splitBuses, splitFactors, cleanupDelaySums]
	}
}


