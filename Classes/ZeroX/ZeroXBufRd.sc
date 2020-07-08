
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


AbstractZeroXBufRd : UGen {

	*checkBufMix { |bufMix, bufSize|
		var outSize;
		bufMix.isNil.if {
			bufMix = (0..bufSize-1);
			outSize = bufSize;
		}{
			bufMix.isKindOf(SequenceableCollection).if {
				outSize = bufMix.size
			}{
				outSize = 1;
				bufMix = bufMix.asArray
			}
		};
		^[bufMix, outSize]
	}

	*unbubbleInputIfNecessary { |... argList|
		^argList.collect { |item|
			((item.isKindOf(SequenceableCollection)) and: { item.size == 1 }).if {
				item[0]
			}{
				item
			};
		}
	}

	// restrict to possible drate arguments
	*checkForWrongDrateInput { |maxArgSize ... args|

		if ((maxArgSize > 1) and: {
			args.any { |x|
				x.isKindOf(DUGen) or: {
					x.isKindOf(SequenceableCollection) and: {
						// do deep check, but only for DUGens
						(x.size < maxArgSize) and: { x.flat.any { |y| y.isKindOf(DUGen) } }
					}
				}
			}
		}) {
			SimpleInitError("For multichannel expansion demand rate " ++
				"ugens, that are needed more than once, have to be " ++
				"wrapped into Functions, see help file"
			).class_(this).throw
		};
	}

	*dUniquifyIfNecessary { |item, dUniqueBufSize|
		^case
		{ item.isKindOf(DUGen) }{ Dunique(item, dUniqueBufSize) }
		{ item.isKindOf(UGen) }{ item }
		{ true }{ Dstutter(inf, item) }
	}
}


ZeroXBufRd : AbstractZeroXBufRd {

	*ar { |sndBuf, zeroXBuf, bufMix, zeroX = 0, power = 1, mul = 1, add = 0, rate = 1, rateMul = 1, dir = 1,
			interpl = 4, dUniqueBufSize = 1048576, length = inf, maxTime = inf, att = 0, rel = 1,
			curve = -4, doneAction = 0|
		var thisX, nextX, baseX, sndBufIndex, phase, phaseTrig, phaseOffset, count, time,
			zeroXDiff, rateSig, rateMulSig, dirSig, sndBufIndexSig, bufSize, zeroXBufSize,
			outSize, args, sig, env, sndBufs, zeroXBufs;

		args = [
			zeroX, power, mul, add, rate, rateMul, dir, interpl, dUniqueBufSize,
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
		#zeroX, power, mul, add, rate, rateMul, dir, interpl, dUniqueBufSize,
			length, maxTime, att, rel, curve, doneAction =
				this.unbubbleInputIfNecessary(*args);
		this.checkForWrongDrateInput(outSize, zeroX, power, mul, rateMul, add, rate, dir);
		#zeroX, power, mul, add, rate, rateMul, dir, interpl, dUniqueBufSize =
			[zeroX, power, mul, add, rate, rateMul, dir, interpl, dUniqueBufSize]
				.collect { |x| x.miSC_Dmultiply2(outSize) };

		// need everything in outSize
		mul = outSize.collect { |i| mul.miSC_maybeWrapAt(i) };
		add = outSize.collect { |i| add.miSC_maybeWrapAt(i) };
		power = outSize.collect { |i| power.miSC_maybeWrapAt(i) };

		dUniqueBufSize = outSize.collect { |i| dUniqueBufSize.miSC_maybeWrapAt(i) };
		length = outSize.collect { |i| length.miSC_maybeWrapAt(i) };
		maxTime = outSize.collect { |i| maxTime.miSC_maybeWrapAt(i) };
		att = outSize.collect { |i| att.miSC_maybeWrapAt(i) };
		rel = outSize.collect { |i| rel.miSC_maybeWrapAt(i) };
		curve = outSize.collect { |i| curve.miSC_maybeWrapAt(i) };
		doneAction = outSize.collect { |i| doneAction.miSC_maybeWrapAt(i) };

		// interpl always refers to sndBufs, not to bufMix size
		interpl = bufSize.collect { |i| interpl.miSC_maybeWrapAt(i) };

		// begin of the subtle part:
		// drate ugens are polled more than once,
		// if they are polled in sync we can "multiply" them with Dstutter
		// otherwise we need Dunique, this is the case for rate and sndBufIndex (bufMix),
		// which are polled again after the half waveset length for which they are needed

		rate = outSize.collect { |i|
			var r = rate.miSC_maybeWrapAt(i);
			this.dUniquifyIfNecessary(r, dUniqueBufSize[i]);
		};
		rateMul = outSize.collect { |i|
			var rMul = rateMul.miSC_maybeWrapAt(i);
			this.dUniquifyIfNecessary(rMul, dUniqueBufSize[i]);
		};
		dir = outSize.collect { |i|
			var d = dir.miSC_maybeWrapAt(i);
			this.dUniquifyIfNecessary(d, dUniqueBufSize[i]);
		};

		zeroXBufs = zeroXBuf.asArray;
		sndBufs = sndBuf.asArray;

		// buffer switch option with bufMix

		sndBufIndex = bufMix.asArray.collect { |b, i|
			this.dUniquifyIfNecessary(b, dUniqueBufSize[i]);
		};

		// determine distance between zero crossings
		zeroX = outSize.collect { |i| Dstutter(2, zeroX.miSC_maybeWrapAt(i)) };

		thisX = outSize.collect { |i|
			Dstutter(2, Dbufrd(Dswitch1(zeroXBufs, sndBufIndex[i]), zeroX[i]))
		};

		nextX = outSize.collect { |i|
			Dstutter(2, Dbufrd(Dswitch1(zeroXBufs, sndBufIndex[i]), zeroX[i] + 1))
		};

		// if dir = -1, baseX is second zero crossing
		baseX = Dstutter(1, thisX * (1 + dir) + (nextX * (1 - dir)) / 2);
		// take half wavesets of minimum length 2, otherwise no trigger
		zeroXDiff = Dstutter(1, max((nextX - thisX / (rate * rateMul)).abs.round, 2));

		// trigger for syncing, add 1 to baseX as zero should trigger too ...
		phaseTrig = TDuty.ar(SampleDur.ir * zeroXDiff, 0, baseX + 1);
		// ... need value again, correct
		phaseOffset = Latch.ar(phaseTrig - 1, phaseTrig);

		count = PulseCount.ar(phaseTrig);
		time = Sweep.ar(Impulse.ar(0));

		env = EnvGen.ar(
			Env.asr(att, 1, rel, curve),
			(count <= length) * (time <= maxTime),
			doneAction: doneAction
		);

		// Demand doesn't multichannel expand
		rateSig = phaseTrig.collect { |x, i| Demand.ar(x, 0, rate[i]) };
		rateMulSig = phaseTrig.collect { |x, i| Demand.ar(x, 0, rateMul[i]) };
		dirSig = phaseTrig.collect { |x, i| Demand.ar(x, 0, dir[i]) };

		// phasor for waveset reading
		phase = Sweep.ar(
			phaseTrig,
			SampleRate.ir * rateSig * rateMulSig * dirSig
		) + phaseOffset;

		power = power.collect { |p, i| p.isNumber.if { p }{ Demand.ar(phaseTrig[i], 0, p) } };
		mul = mul.collect { |m, i| m.isNumber.if { m }{ Demand.ar(phaseTrig[i], 0, m) } };
		add = add.collect { |a, i| a.isNumber.if { a }{ Demand.ar(phaseTrig[i], 0, a) } };

		sndBufIndexSig = phaseTrig.collect { |x, i| Demand.ar(x, 0, sndBufIndex[i]) };

		sig = outSize.collect { |i|
			var parBuf;
			bufMix[i].isNumber.if {
				BufRd.ar(1, sndBufs[bufMix[i]], phase[i], 0, interpl[bufMix[i]])
			}{
				// in case of switched buffers we must select from all buffer readers
				// (BufRd doesn't allow ar buffer switching)
				parBuf = bufSize.collect { |j| BufRd.ar(1, sndBufs[j], phase[i], 0, interpl[j]) };
				Select.ar(sndBufIndexSig[i], parBuf)
			}
		};

		sig = sig ** power.abs * mul + add * env;
		^sig.unbubble
	}
}

