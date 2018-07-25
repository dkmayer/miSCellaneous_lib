
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


AddEventTypes_PbindFx {

	*initClass {
		StartUp.add {
			AddEventTypes_PbindFx.makeZeroSynthDef;
			AddEventTypes_PbindFx.makeSplitZeroSynthDef;
			AddEventTypes_PbindFx.makeSplitSynthDefs(8, 8);
		};
		AddEventTypes_PbindFx.makeEventType_pbindFx;
	}

	*makeEventType_pbindFx {
		Event.addEventType(\pbindFx,
			Event.default.eventTypes[\note] <> { |server|
				// The array ~fxOrder contains the topological order of effect graph as indices,
				// 0 (src) is not contained, the smallest effect index is 1.
				// The dictionary ~fxPredecessors contains the fx graph's predecessors,
				// also those of \o (out).
				// The dictionary ~fxSuccessors contains the fx graph's successors,
				// also those of 0 (src).

				var fxNum, // size of ~fxOrder
					fxBuses, // array of fx input buses (contains nils if there is no in !),
					// size = fxNum
					splitBuses, // array of split synth input buses,
						// contains nils for effects without split,
						// size = fxNum, but different to fxBuses the first item refers to the source
						// the last fx in topological order never needs a split
					splitFactors, // array of split synths' split factors, 1 means no split, size = fxNum
					fxOrderPositions, // dictionary of fx index position in ~fxOrder (+ 1, contains 0 too)
					firstFxIds, lastFxIds, 	// these arrays (size = fxNum) keep track of zero synth ordering
						// in fxGroup, needed as zero synths are established before fx synths,
						// relevant is first and last zero synth per fx slot

					cleanupDelaySums, // array of cleanup delays after src and fxs,
						// therefore contains fxNum + 1 items,
						// this array is ordered in the case of an fx sequence but not for a general fx graph,
						// thus we need a variable for this:
					latestCleanupIndex,
					withFxs, startFxBundle, startZeroBundle = [], cleanupClockTempo,
					baseLatency, hasGate, gateDelay, cleanupDelta, skipjack, startTime, zeroSynthOverlap,
					fxGroupId, srcGroupId, startSplitBundle, startSplitZeroBundle, splitSynthIds;

				~cleanupDelay = ~cleanupDelay ?? { PbindFx.defaultSourceCleanupDelay };
				~cleanupDt = ~cleanupDt ?? { PbindFx.defaultCleanupDt };
				~cleanupClock = ~cleanupClock ?? { SystemClock };
				~freePerGroup = ~freePerGroup ?? { false };

				cleanupClockTempo = (~cleanupClock === SystemClock).if { 1 }{ ~cleanupClock.tempo };
				zeroSynthOverlap = server.options.blockSize / server.sampleRate;

				// unifying ~fxOrder order data (with topo order etc) has been defined in
				// AbstractPbindFx / miSC_getFxOrderData

				~fxOrder = ~fxOrder.asArray;
				fxOrderPositions = IdentityDictionary();
				fxOrderPositions.put(0, 0);
				~fxOrder.do { |x,i| fxOrderPositions.put(x, i+1) };

				fxNum = ~fxOrder.size;
				withFxs = ((fxNum == 1) && (~fxOrder[0] == 0)).not;

				withFxs.if {
					cleanupDelaySums = [~cleanupDelay];
					baseLatency = (~latency ?? { server.latency });

					// fxGroup: group for all synths related to the event
					// srcGroup: placed at top of fxGroup
					// in general this node order wil be established (index shift from code):

					// fxGroup:
					// 		srcGroup:
					// 			(splitZero #1	writes 0 to splitbus, in case src will write to such)
					// 			zero #1			writes 0 to bus(es), to which src (or split of src) will write
					// 			src				writes to whatever buses
					// 			(split #1		in case src writes to more than one bus)
					// 		(splitZero #2		writes 0 to splitbus, in case fx #1 will write to such)
					// 		(zero #2			writes 0 to bus(es), to which fx #1 (or split of fx #1) will write)
					// 		fx #1				reads from buses #1, writes to whatever buses
					// 		(split #2			in case fx #1 writes to more than one bus)

					// 		...

					// 		(splitZero #n		writes 0 to splitbus, in case fx #n-1 will write to such)
					// 		(zero #n			writes 0 to bus(es), to which fx #n-1 (or split of fx #n-1) will write)

					// 		fx #n-1				reads from whatever, writes to whatever
					// 		(split #n			in case fx #n-1 writes to more than one bus)
					// 		fx #n				reads from whatever, writes to out

					fxGroupId = server.nextNodeID;
					srcGroupId = server.nextNodeID;
					server.sendBundle(nil,
						[21, fxGroupId, ~addAction ?? { 0 }, ~group.value.asNodeID],
						[21, srcGroupId, 0, fxGroupId]
					);

					// Per convention \cleanupDelay of src synth indicates:
					// maximum release time in case of a gated envelope
					// maximum synth duration in case of a fixed length envelope.
					// gateDelay adds delay times depending on env type,
					// it refers to beats and thus will be added to ~timingOffset.

					hasGate = SynthDescLib.global[~instrument].controlNames.includes(\gate);
					gateDelay = hasGate.if { ~sustain.value }{ 0 };

					// reserve buses with right sizes,
					// this looks at fx SynthDef's in sizes

					#fxBuses, splitBuses, splitFactors, cleanupDelaySums = ~fxOrder.miSC_getFxBusData(
						~fxPredecessors, ~fxSuccessors, fxOrderPositions,
						~fxEvents, ~instrument, ~otherBusArgs, ~cleanupDelay, server
					);

					latestCleanupIndex = cleanupDelaySums.maxIndex;
					// avoid cleanup ambivalence with two latest cleanups
					cleanupDelaySums[latestCleanupIndex] = cleanupDelaySums[latestCleanupIndex] + 0.01;

					// startFxBundle and startZerobundle are just collected and scheduled afterwards
					// freeing with freeFxBundle and freeZeroBundle is either
					// (1) scheduled at once by freeing fxGroup (with option ~freePerGroup == true) or
					// (2) scheduled separately (with option ~freePerGroup == false)
					// (1) and (2) is done within the methods

					#startZeroBundle, startSplitBundle, startSplitZeroBundle, firstFxIds, lastFxIds, splitSynthIds =
						~fxOrder.miSC_makeStartZeroBundle(
							~fxPredecessors, ~fxSuccessors, fxOrderPositions, fxBuses, splitBuses, splitFactors,
							fxNum, srcGroupId, fxGroupId, baseLatency, cleanupDelaySums, latestCleanupIndex,
							gateDelay, ~out, ~freePerGroup, ~timingOffset, ~lag, zeroSynthOverlap, server
					);

					startFxBundle = ~fxOrder.miSC_makeStartFxBundle(~fxSuccessors,
						fxOrderPositions, fxBuses, splitBuses, splitFactors, ~fxEvents, srcGroupId,
						fxGroupId, firstFxIds, lastFxIds, baseLatency, cleanupDelaySums, latestCleanupIndex,
						gateDelay, ~out, ~freePerGroup, ~timingOffset, ~lag, server
					);

					// now start bundles that have been collected
					// schedule zero synths, split synths and split zero synths slightly before fxs synths
					schedBundleArrayOnClock(
						~timingOffset, thisThread.clock, startZeroBundle, ~lag, server,
						baseLatency - zeroSynthOverlap
					);

					schedBundleArrayOnClock(
						~timingOffset, thisThread.clock, startSplitZeroBundle, ~lag, server,
						baseLatency - (zeroSynthOverlap * 0.75)
					);

					schedBundleArrayOnClock(
						~timingOffset, thisThread.clock, startSplitBundle, ~lag, server,
						baseLatency - (zeroSynthOverlap * 0.5)
					);

					// schedule fx synths
					schedBundleArrayOnClock(
						~timingOffset, thisThread.clock, startFxBundle, ~lag, server, baseLatency
					);

					// free buses of fx chain after summed cleanup time

					cleanupDelta = baseLatency + cleanupDelaySums.last * cleanupClockTempo;
					startTime = ~cleanupClock.seconds;

					skipjack = SkipJack(
						dt: ~cleanupDt * cleanupClockTempo,
						clock: ~cleanupClock
					);
					skipjack.stopTest = {
						(skipjack.clock.seconds - startTime > cleanupDelta).if {
							fxBuses.do(_.free);
							splitBuses.do(_.free);
							// skipjacks stopped by stopTest are not automatically removed
							skipjack.miSC_cleanup;
							true
						}{
							false
						};
					};

					// route source synth out into fx chain

					// source synth(s), which will refer to ~group and ~addAction,
					// will be placed at tail of srcGroup or before split synth (also in srcGroup),
					// so anyway before fx synths

					(splitFactors[0] > 1).if {
						~out = splitBuses[0].index;
						~group = [splitSynthIds[0]];
						~addAction = \addBefore;

					}{
						// ~out = fxBuses[0].index;
						~out = fxBuses[fxOrderPositions[~fxSuccessors[0].first] - 1].index;
						~group = [srcGroupId];
						~addAction = \addToTail;
					};

				};
				server;
			}
		)
	}

	*makeZeroSynthDef {
		SynthDef(\pbindFx_zero, { |out|
		 	ReplaceOut.ar(out, DC.ar(0))
		}, [\ir]).writeDefFile;
	}

	// like \pbindFx_zero, but makes order more clear when doing s.queryAllNodes while running
	*makeSplitZeroSynthDef {
		SynthDef(\pbindFx_splitZero, { |out|
		 	ReplaceOut.ar(out, DC.ar(0))
		}, [\ir]).writeDefFile;
	}

	*makeSplitSynthDef { |splitNum, channelNum|
		SynthDef("pbindFx_split_" ++ splitNum ++ "x" ++ channelNum, { |in|
		 	var out = \out.kr(0!splitNum);
			Out.ar(out, In.ar(in, channelNum))
		}, [\ir]).writeDefFile;
	}

	*makeSplitSynthDefs { |maxSplitNum, maxChannelNum|
		for(2, maxSplitNum, { |i|
			for(1, maxSplitNum, { |j|
				this.makeSplitSynthDef(i,j)
			})
		});
	}

}


+SkipJack {
	miSC_cleanup {
		all.remove(this);
		CmdPeriod.remove(this);
		if( verbose ) { ("SkipJack" + name + "stopped.").postcln };
	}
}


+SequenceableCollection {
	miSC_collectEvalWithoutKeyPairs { |syms, ev|
		var coll;
		this.pairsDo { |k,v| syms.includes(k).not.if { coll = coll.addAll([k, ev.use { v.() }]) } };
		^coll
	}
}





