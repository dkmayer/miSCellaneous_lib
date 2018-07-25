
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


+SequenceableCollection {

	miSC_makeStartFxBundle { |successors, fxOrderPositions, fxBuses, splitBuses,
		splitFactors, fxEvents, srcGroupId, fxGroupId, firstFxIds, lastFxIds, baseLatency,
		cleanupDelaySums, latestCleanupIndex, gateDelay, out, freePerGroup, timingOffset, lag, server|

		var outBus, msgFunc, hasAmp, hasFreq, removeSyms, actionNum, referenceId,
			fxPairs, fxBundle, fxIds, fxEvent, startFxBundle, freeFxBundle;

		// fxOrder is receiver
		this.do { |fxIndex, i|
			fxEvent = fxEvents[fxIndex-1];

			// prepare fxPairs from msgFunc
			// fx should not have default amp and freq

			msgFunc = SynthDescLib.global[fxEvent[\fx]].msgFunc;
			fxPairs = fxEvent.use { msgFunc.valueEnvir };

			hasAmp = #[amp, db].any { |x| fxEvent.keys.includes(x) };
			hasFreq = #[degree, note, midinote, freq].any { |x| fxEvent.keys.includes(x) };

			removeSyms = [\out] ++ (hasAmp.not.if { \amp }) ++ (hasFreq.not.if { \freq });
			fxPairs = fxPairs.miSC_collectEvalWithoutKeyPairs(removeSyms, fxEvent);

			outBus = (i < (this.size-1)).if {
				// index shift as split arrays also refer to src
				(splitFactors[i+1] > 1).if {
					splitBuses[i+1].index
				}{
					(successors[fxIndex].first == \o).if {
						out.asControlInput
					}{
						fxBuses[fxOrderPositions[successors[fxIndex].first] - 1].index
					}
				}
			}{
				out.asControlInput
			};

			fxBundle = fxPairs.asArray.flop;
			fxIds = fxIds.add({ server.nextNodeID } ! (fxBundle.size));

			// if fx has an array value do generate multiple fx

			fxBundle.do { |fxMsg, j|

				#firstFxIds, lastFxIds, referenceId, actionNum = i.miSC_fxIdRank(
					firstFxIds, lastFxIds, fxIds.last[j], srcGroupId
				);

				startFxBundle = startFxBundle ++ [
					([\s_new, fxEvent[\fx], fxIds.last[j], actionNum, referenceId] ++
					fxMsg ++ (fxBuses[i].isNil).if { [] }{ [\in, fxBuses[i].index] } ++
					[\out, outBus]).asOSCArgArray
				];

			};

			// latest fx synth per chain will be freed anyway by group (i == latestCleanupIndex),
			// others will be freed before if ~freePerGroup == true

			((i == (latestCleanupIndex - 1)) or: { freePerGroup.not }).if {
				freeFxBundle = ((i == (latestCleanupIndex - 1)).if {
					[11, fxGroupId]
				}{
					[11, fxIds.last.last]
				}).flop;

				schedBundleArrayOnClock(timingOffset + gateDelay,
					thisThread.clock, freeFxBundle, lag, server,
					baseLatency + cleanupDelaySums[i+1]
				)
			};
		};

		^startFxBundle
	}




	miSC_makeStartZeroBundle { |predecessors, successors, fxOrderPositions, fxBuses, splitBuses,
		splitFactors, fxNum, srcGroupId, fxGroupId, baseLatency, cleanupDelaySums, latestCleanupIndex,
		gateDelay, out, freePerGroup, timingOffset, lag, zeroSynthOverlap, server|

		var index, numChannels, fxZeroSynthIds, actionNum, referenceId, firstFxIds,
			lastFxIds, predecessorsI, prependIndex, freeZeroBundle, startZeroBundle,
			startSplitBundle, splitSynthIds, freeSplitBundle, fxOutIndices, splitName, channelNum,
			startSplitZeroBundle, fxSplitZeroSynthIds, freeSplitZeroBundle;

		// this data structure keeps track of zero synth ordering in *fxGroup*,
		// needed as zero synths are established before fx synths,
		// relevant is first and last zero synth per fx slot

		firstFxIds = {} ! fxNum;
		lastFxIds = {} ! fxNum;

		this.do { |fxIndex, i|
			predecessorsI = predecessors[fxIndex].asArray;

			(predecessorsI.size != 0).if {
				// prepare buses and zero synth ids
				index = fxBuses[i].index;
				numChannels = fxBuses[i].numChannels;
				fxZeroSynthIds = numChannels.collect { server.nextNodeID };

				predecessorsI.includes(0).if {

					// if fx reads from src, zero synth(s) must be before src (head of src group)
					actionNum = 0;
					startZeroBundle = startZeroBundle ++ fxZeroSynthIds.collect { |id, j|
						[\s_new, \pbindFx_zero, id, actionNum, srcGroupId, "out", index + j]
					};
				}{
					// find first fx in order which writes to this fx,
					// zero synth(s) must must be before it
					actionNum = 2;

					prependIndex = fxNum-1;
					predecessorsI.do { |x|
						(fxOrderPositions[x] - 1 < prependIndex).if {
							prependIndex = fxOrderPositions[x] - 1
						}
					};

					fxZeroSynthIds.do { |id, j|
						#firstFxIds, lastFxIds, referenceId, actionNum = prependIndex.miSC_zeroIdRank(
							firstFxIds, lastFxIds, id, fxGroupId
						);

						startZeroBundle = startZeroBundle ++ [
							[\s_new, \pbindFx_zero, id, actionNum, referenceId, "out", index + j]
						]
					}
				}
			};

			// for latest fx synth (i == (latestCleanupIndex-1)) zero synth(s) are freed by group (freeFxBundle)
			// other zero synths will be freed separately except if ~freePerGroup == true,
			// freeing is scheduled with overlap

			((i != (latestCleanupIndex - 1)) and: { freePerGroup.not and: { (predecessorsI.size != 0) } }).if {
				freeZeroBundle = (fxZeroSynthIds.collect([11, _]));
				schedBundleArrayOnClock(timingOffset + gateDelay,
					thisThread.clock, freeZeroBundle, lag, server,
					baseLatency + cleanupDelaySums[i+1] + zeroSynthOverlap
				);
			};
		};

		splitSynthIds = {} ! fxNum;

		splitFactors.do { |splitFactor, i|

			(splitFactor > 1).if {
				splitSynthIds[i] = server.nextNodeID;
				fxOutIndices = successors[i].asArray.collect { |s|
					(s == \o).if {
						out.asControlInput
					}{
						fxBuses[fxOrderPositions[s] - 1].index
					}
				};

				channelNum = fxBuses[fxOrderPositions[successors[i].first] - 1].numChannels;
				splitName =  \pbindFx_split_ ++ splitFactor.asString ++ "x" ++ channelNum.asString;

				(i == 0).if {
					startSplitBundle = startSplitBundle ++ [
						[\s_new, splitName, splitSynthIds[i], 1, srcGroupId,
							"in", splitBuses[i].index, "out", fxOutIndices].asOSCArgArray
					];
					fxSplitZeroSynthIds = splitBuses[i].numChannels.collect { server.nextNodeID };
					fxSplitZeroSynthIds.do { |id, j|
						startSplitZeroBundle = startSplitZeroBundle ++ [
							[\s_new, \pbindFx_splitZero, id, 0, srcGroupId,
								"out", splitBuses[i].index + j].asOSCArgArray
						]
					}
				}{
					#firstFxIds, lastFxIds, referenceId, actionNum = i.miSC_fxIdRank(
						firstFxIds, lastFxIds, splitSynthIds[i], fxGroupId
					);

					startSplitBundle = startSplitBundle ++ [
						[\s_new, splitName, splitSynthIds[i], actionNum, referenceId,
							"in", splitBuses[i].index, "out", fxOutIndices].asOSCArgArray
					];

					fxSplitZeroSynthIds = splitBuses[i].numChannels.collect { server.nextNodeID };

					fxSplitZeroSynthIds.do { |id, j|
						#firstFxIds, lastFxIds, referenceId, actionNum = (i-1).miSC_zeroIdRank(
							firstFxIds, lastFxIds, id, fxGroupId
						);

						startSplitZeroBundle = startSplitZeroBundle ++ [
							[\s_new, \pbindFx_splitZero, id, actionNum, referenceId, "out",
								splitBuses[i].index + j]
						]
					}
				};

				// free split and splitZero synths
				// last splitFactor must not be other than 1

				((i != latestCleanupIndex) and: { freePerGroup.not }).if {
					freeSplitBundle = [[11, splitSynthIds[i]]];
					schedBundleArrayOnClock(timingOffset + gateDelay,
						thisThread.clock, freeSplitBundle, lag, server,
						baseLatency + cleanupDelaySums[i]
					);
					freeSplitZeroBundle = (fxSplitZeroSynthIds.collect([11, _]));
					schedBundleArrayOnClock(timingOffset + gateDelay,
						thisThread.clock, freeSplitZeroBundle, lag, server,
						baseLatency + cleanupDelaySums[i] + zeroSynthOverlap
					);
				}
			}
		};

		^[startZeroBundle, startSplitBundle, startSplitZeroBundle, firstFxIds, lastFxIds, splitSynthIds];
	}

}
