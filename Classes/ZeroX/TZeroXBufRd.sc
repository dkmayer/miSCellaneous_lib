
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


TZeroXBufRd : AbstractZeroXBufRd {

	*ar { |sndBuf, zeroXBuf, bufMix, trig, zeroX = 0, xNum = 1, xRep = 1, power = 1, mul = 1, add = 0,
			rate = 1, dir = 1, interpl = 4, overlapSize = 10, length = inf, maxTime = inf,
			att = 0, rel = 1, curve = -4, doneAction = 0|
		var thisX, nextX, baseX, sndBufIndex, phase, phaseTrig, phaseOffset, time, env, sndBufs,
			zeroXBufs, zeroXDiff, rateSig, dirSig, sndBufIndexSig, bufSize, zeroXBufSize,
			outSize, args, trigCount, sig;

		args = [
			zeroX, xNum, xRep, power, mul, add, rate, dir, interpl, overlapSize,
			length, maxTime, att, rel, curve, doneAction
		];

		bufSize = (sndBuf.size == 0).if { 1 }{ sndBuf.size };
		zeroXBufSize = (zeroXBuf.size == 0).if { 1 }{ zeroXBuf.size };

		(zeroXBufSize != bufSize).if {
			SimpleInitError("sizes of sndBuf and zeroXBuf (single channel buffers!) must correspond")
				.class_(ZeroXBufWr).throw
		};

		// if bufMix is undefined then outSize = bufSize
		// else outSize is given by bufMix size
		#bufMix, outSize = this.checkBufMix(bufMix, bufSize);

		// args might be passd as Function of drate ugens, then expand
		#zeroX, xNum, xRep, power, mul, add, rate, dir, interpl, overlapSize,
			length, maxTime, att, rel, curve, doneAction =
				this.unbubbleInputIfNecessary(*args);

		this.checkForWrongDrateInput(outSize, zeroX, xNum, xRep, power, mul, add, rate, dir);
		#zeroX, xNum, xRep, power, mul, add, rate, dir =
			[zeroX, xNum, xRep, power, mul, add, rate, dir]
				.collect { |x| x.miSC_Dmultiply2(outSize) };

		// need everything in outSize
		mul = outSize.collect { |i| mul.miSC_maybeWrapAt(i) };
		add = outSize.collect { |i| add.miSC_maybeWrapAt(i) };
		power = outSize.collect { |i| power.miSC_maybeWrapAt(i) };

		trig = outSize.collect { |i| trig.miSC_maybeWrapAt(i) };
		xNum = outSize.collect { |i| xNum.miSC_maybeWrapAt(i) };
		xRep = outSize.collect { |i| xRep.miSC_maybeWrapAt(i) };

		trigCount = outSize.collect { |i| PulseCount.ar(trig[i]) - 1 };

		overlapSize = outSize.collect { |i| overlapSize.miSC_maybeWrapAt(i) };
		length = outSize.collect { |i| length.miSC_maybeWrapAt(i) };
		maxTime = outSize.collect { |i| maxTime.miSC_maybeWrapAt(i) };
		att = outSize.collect { |i| att.miSC_maybeWrapAt(i) };
		rel = outSize.collect { |i| rel.miSC_maybeWrapAt(i) };
		curve = outSize.collect { |i| curve.miSC_maybeWrapAt(i) };
		doneAction = outSize.collect { |i| doneAction.miSC_maybeWrapAt(i) };

		// interpl always refers to sndBufs, not to bufMix size
		interpl = bufSize.collect { |i| interpl.miSC_maybeWrapAt(i) };

		time = Sweep.ar(Impulse.ar(0));

		env = EnvGen.ar(
			Env.asr(att, 1, rel, curve),
			(trigCount + 1 <= length) * (time <= maxTime),
			doneAction: doneAction
		);

		// core:
		// a bit easier than with ZeroXBufRd, Dunique isn't needed here.
		// Some drate ugens are polled more than once,
		// we can "multiply" them with Dstutter.

		rate = outSize.collect { |i|
			var r = rate.miSC_maybeWrapAt(i);
			r.isNumber.if { Dstutter(inf, r) }{ Dstutter(2, r) };
		};
		dir = outSize.collect { |i|
			var d = dir.miSC_maybeWrapAt(i);
			d.isNumber.if { Dstutter(inf, d) }{ Dstutter(3, d) };
		};

		zeroXBufs = zeroXBuf.asArray;
		sndBufs = sndBuf.asArray;

		// buffer switch option with bufMix
		sndBufIndex = bufMix.collect { |b|
			b.isNumber.if { Dstutter(inf, b) }{ Dstutter(3, b) }
		};

		// determine distance between zero crossings
		zeroX = outSize.collect { |i| Dstutter(2, zeroX.miSC_maybeWrapAt(i)) };

		thisX = outSize.collect { |i|
			Dstutter(2, Dbufrd(Dswitch1(zeroXBufs, sndBufIndex[i]), zeroX[i]));
		};
		nextX = outSize.collect { |i|
			Dstutter(2, Dbufrd(Dswitch1(zeroXBufs, sndBufIndex[i]), zeroX[i] + xNum[i]));
		};

		// if dir = -1, base is second zero crossing
		zeroXDiff = Dstutter(1, max((nextX - thisX / rate).abs.round), 2);
		baseX = Dstutter(1, thisX * (1 + dir) + (nextX * (1 - dir)) / 2);

		sig = outSize.collect { |i|
			overlapSize[i].collect { |j|
				var localTrig, localZeroXDiff, localPhaseOffset, localRate, localDir,
					localXRep, localOff, localGate, localGateDur, localPhase, localSndBufIndex,
					localPower, localMul, localAdd, parBuf;

				localTrig = trig[i] * ((trigCount[i] % (overlapSize[i]) - DC.ar(j)).abs < 0.01);

				localZeroXDiff = Demand.ar(localTrig, 0, zeroXDiff[i]);
				localPhaseOffset = Demand.ar(localTrig, 0, baseX[i]);
				localRate = Demand.ar(localTrig, 0, rate[i]);
				localDir = Demand.ar(localTrig, 0, dir[i]);
				localXRep = xRep[i].isNumber.if { xRep[i] }{ Demand.ar(localTrig, 0, xRep[i]) };

				// gate for wavesets, workaround with Sweep because of Trig1 init issue
				localGateDur = localXRep * localZeroXDiff * SampleDur.ir;
				localGate = Sweep.ar(localTrig) < localGateDur;

				// the combination of localGate and Sweep + modulo implements repetitions
				localPhase = Sweep.ar(
					localTrig,
					SampleRate.ir * localRate
				) % (localZeroXDiff * localRate) * localDir.sign + localPhaseOffset;

				localSndBufIndex = Demand.ar(localTrig, 0, sndBufIndex[i]);

				localPower = power[i].isNumber.if { power[i] }{ Demand.ar(localTrig, 0, power[i]) };
				localMul = mul[i].isNumber.if { mul[i] }{ Demand.ar(localTrig, 0, mul[i]) };
				localAdd = add[i].isNumber.if { add[i] }{ Demand.ar(localTrig, 0, add[i]) };

				bufMix[i].isNumber.if {
					BufRd.ar(1, sndBufs[bufMix[i]], localPhase, 0, interpl[bufMix[i]])
				}{
					// in case of switched buffers we must select from all buffer readers
					// (BufRd doesn't allow ar buffer switching)
					parBuf = bufSize.collect { |k| BufRd.ar(1, sndBufs[k], localPhase, 0, interpl[k]) };
					Select.ar(localSndBufIndex, parBuf)
				} ** localPower * localMul + localAdd * localGate;
			}.sum
		} * env;

		^sig.unbubble
	}

}


+Object {
	miSC_Dmultiply2 { ^this }
}

+Function {
	miSC_Dmultiply2 { |n| ^this ! n }
}

+SequenceableCollection {
	miSC_Dmultiply2 { |n| ^{ |i| this.wrapAt(i) } ! n }
}
