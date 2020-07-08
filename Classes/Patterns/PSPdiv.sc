
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


PSPdiv : Pspawner {
	classvar <>shiftDelta = 1e-8;
	var <>pulse, <>evPat, <>div, <>divBase, <>divType;

	*new { |pulse, evPat, div = 1, divBase = 1, divType = \seq|
		^super.newCopyArgs.pspDivInit(pulse, evPat, div, divBase, divType)
	}

	pspDivInit { |pulse, evPat, div, divBase, divType|
		var divPS, divBasePS, divTypePS, sizes, maxSize;

		pulse.isKindOf(Number).if { pulse = Pn(pulse) };
		evPat = evPat.asArray;
		div = div.asArray;
		divBase = divBase.asArray;
		divType = divType.asArray;
		// div, divBase and divTape: size must be equal and equal 1 or evPat.size

		sizes = [evPat, div, divBase, divType].collect(_.size).asSet;
		maxSize = sizes.maxItem;

		((sizes.size > 2) or: { (sizes.size == 2) && (sizes.includes(1).not) }).if {
			SimpleInitError("sizes of evPat, div, divBase, divType must equal 1 or n > 1").throw;
		};

		routineFunc = { |sp|
			var pulseStr = pulse.asStream;
			var evPats = evPat.miSC_unifySize(maxSize)
				.collect { |p| p.isKindOf(Pattern).if { p.asStream }{ p } };
			var divStrs = div.miSC_unifySize(maxSize).collect { |x| x.asStream };
			var divBaseStrs = divBase.miSC_unifySize(maxSize).collect(_.asStream);
			var divTypeStrs = divType.miSC_unifySize(maxSize).collect(_.asStream);
			var pulseCount = 0;
			var queueCount = 0;
			// queue orders queueCount
			var queue = PriorityQueue.new;
			var nextPulseDurs = [];
			var nextDiv, nextDivBase, nextDivType, s,
				nextDivs, nextDivBases, nextDivTypes,
				currentIndices, nextDivBaseDur, maxDivBase,
				nextEvPatDurs, nextQueueStep, nextPulseStep,
				nextPulseDurSum, nextPulseDurSums, suspendIndices;

			(0..evPats.size-1).do { |i| queue.put(0, i) };

			block { |break| loop {
				s = 0;
				nextDiv = nil;
				nextDivBase = nil;
				nextDivType = nil;
				nextDivs = nil;
				nextDivBases = nil;
				nextDivTypes = nil;
				currentIndices = nil;
				nextDivBaseDur = nil;
				maxDivBase = nil;
				nextEvPatDurs = nil;
				nextQueueStep = nil;
				nextPulseStep = nil;
				nextPulseDurSum = nil;
				nextPulseDurSums = nil;
				suspendIndices = nil;

				while { queueCount == queue.topPriority }{
					currentIndices = currentIndices.asArray ++ (queue.pop);
				};

				(currentIndices.size != 0).if {
					currentIndices.sort;

					maxDivBase = 0;

					currentIndices.do { |i|
						nextDiv = divStrs[i].next;
						nextDivBase = divBaseStrs[i].next;
						nextDivType = divTypeStrs[i].next;

						((nextDiv.isNil) || (nextDivBase.isNil) || (nextDivType.isNil)).if {
							suspendIndices = suspendIndices.add(i);
						}{

							nextDivs = nextDivs.add(nextDiv);
							nextDivBases = nextDivBases.add(nextDivBase);
							nextDivTypes = nextDivTypes.add(nextDivType);

							(nextDivBase > maxDivBase).if { maxDivBase = nextDivBase };

							(evPats[i].isKindOf(Function) or:
								{ (evPats[i].state == 0) || (evPats[i].state == 5) }).if {
								queue.put(
									(nextDivType == 'par').if { 1 }{ nextDivBase } + queueCount,
									i
								);
							}{
								suspendIndices = suspendIndices.add(i);
							}
						};
					};

					currentIndices.removeAll(suspendIndices);

					(currentIndices.size == 0).if {
						sp.suspendAll;
						break.value;
					};

					(queueCount >= pulseCount).if {
						nextPulseStep = maxDivBase;
						nextPulseDurs = pulseStr.nextN(nextPulseStep);
					}{
						nextPulseStep = maxDivBase - pulseCount + queueCount;
						nextPulseDurs = nextPulseDurs ++
							pulseStr.nextN(nextPulseStep);
					};

					nextQueueStep = (queue.topPriority - queueCount).round.asInteger;

					nextPulseDurSums = nextPulseDurs.collect { |d| s = s + d };
					nextPulseDurSum = nextPulseDurSums[nextQueueStep - 1];

					// sprout subpatterns

					currentIndices.do { |i, j|
						nextDivBaseDur = nextPulseDurSums[nextDivBases[j] - 1];
						nextEvPatDurs = nextEvPatDurs
							.add(this.miSC_calcDivs(nextDivs[j], nextDivBaseDur));

						sp.par(
							Pspawner { |sp|
								(i * shiftDelta).wait;

								sp.par(
									evPats[i].isKindOf(Function).if {
										evPats[i].(
											nextEvPatDurs[i],
											nextDivs[j],
											nextDivBases[j],
											nextDivTypes[j]
										)
									}{
										Pbindf(
											Pfin(nextEvPatDurs[i].size, evPats[i]),
											\dur, Pseq(nextEvPatDurs[j])
										)
									}
								)
							}
						);
					}

				}{
					sp.suspendAll;
					break.value;
				};
				queueCount = queueCount + nextQueueStep;
				pulseCount = pulseCount + nextPulseStep;
				nextPulseDurs = nextPulseDurs.drop(nextQueueStep);
				nextPulseDurSum.wait;
			} }
		};
		^this
	}

	miSC_calcDivs { |div, nextDivBaseDur|
		^case
			{ div.isKindOf(Integer) }{ (nextDivBaseDur / div) ! div }
			{ div.isKindOf(Array) }{ div.normalizeSum * nextDivBaseDur }
			{ div.isKindOf(Function) }{ div.(nextDivBaseDur) }
			{ true }{ "div item of wrong type: must be Integer, Array or Function" }
	}

}



+SequenceableCollection {
	miSC_unifySize { |size|
		^(this.size != size).if { this ++ (this[0] ! (size - 1)) }{ this }
	}
}

+Object {
	miSC_unifySize { |size| ^this ! size }
}

