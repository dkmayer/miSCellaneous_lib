
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


ZeroXBufWr : UGen {

	*ar { |in, sndBuf, zeroXBuf, startWithZeroX = 0, adjustZeroXs = 0, doneAction = 0|
		var size, bufSize, zeroXBufSize, signToggle, sampleCount, zeroX, zeroXCount,
			sampleCountSteps, zeroXPositions, newZeroXs, heldZeroXs, write, out, gate, initTrig;

		size = in.size;

		bufSize = (sndBuf.size == 0).if { 1 }{ sndBuf.size };
		zeroXBufSize = (zeroXBuf.size == 0).if { 1 }{ zeroXBuf.size };

		(zeroXBufSize != bufSize).if {
			SimpleInitError("sizes of sndBuf and zeroXBuf (single channel buffers!) must correspond")
				.class_(ZeroXBufWr).throw
		};

		(max(size, 1) != bufSize).if {
			SimpleInitError("size of in must correpond to number of single channel buffers")
				.class_(ZeroXBufWr).throw
		};

		// signToggle always starts with 1, makes it easier to handle all cases
		initTrig = Impulse.ar(0);
		signToggle = Select.ar(
			Latch.ar(in > 0, initTrig),
			[in <= 0, in > 0]
		);

		// need to start with index 0, thus '- 1'
		sampleCount = Sweep.ar(0, SampleRate.ir) - 1;

		// zeroX indicates triggers for the counter:
		// differentiate behaviour at start and later
		// according to flags startWithZeroX and adjustZeroXs

		// adjustZeroXs = -1: indicate all zeroXs, dont't write sound buffer
		// adjustZeroXs = 0: indicate all zeroXs, write sound buffer
		// adjustZeroXs = 1: indicate all zeroXs, write sound buffer, set to 0 there
		// adjustZeroXs = 2: indicate zeroXs with min distance of 2, set to 0 there

		sndBuf = sndBuf.asArray;
		adjustZeroXs = adjustZeroXs.asArray;

		zeroX = sndBuf.collect { |buf, i|
			var sig, sampleToggle, flag = adjustZeroXs.miSC_maybeWrapAt(i);

			sig = Select.ar(
				sampleCount.sign,
				[
					// with flag 1 always start with zeroX
					DC.ar(startWithZeroX.miSC_maybeWrapAt(i)),
					// slope of signToggle indicates zeroX, if not at start
					Slope.ar(signToggle.miSC_maybeWrapAt(i)).abs
				]
			) > 0;

			(flag == 2).if {
				// take zeroXs with minimum distance 2
				sampleToggle = Duty.ar(SampleDur.ir, sig, Dseq([1, 0], inf));
				sig = sig * sampleToggle
			};

			sig
		};

		// workaround due to a Integrator init bug
		zeroXCount = Integrator.ar(
			Select.ar(
				sampleCount.sign,
				[DC.ar(0), zeroX]
			)
		) + Latch.ar(zeroX, initTrig);
		zeroXCount = zeroXCount.asArray - 1;

		newZeroXs = Select.ar(
			zeroX,
			[DC.ar(0), sampleCount]
		).asArray;

		// if zeroXCount doesn't increase, the position shouldn't either:
		// BufWr overwrites the same buffer position with the same value

		// 1 - zeroX is a trigger for start of same sign
		heldZeroXs = Latch.ar(Delay1.ar(newZeroXs), 1 - zeroX).asArray;

		zeroXPositions = Select.ar(
			zeroX,
			[heldZeroXs, sampleCount]
		).asArray;

		out = in;
		in = in.asArray;
		zeroXBuf = zeroXBuf.asArray;

		write = DC.ar(0);
		sndBuf.do { |buf, i|
			var flag = adjustZeroXs.miSC_maybeWrapAt(i);

			case
			// dont't write sound buffer
			{ flag == -1 }{ }

			// write sound buffer from in signal
			{ flag == 0 }{ write = (BufWr.ar(in[i], buf, sampleCount, 0) <! write) }

			// write sound buffer from in signal, but set to 0 at indicated zeroXs
			{ [1, 2].includes(flag) }{
				write = (
					BufWr.ar(
						Select.ar(zeroX[i], [in[i], DC.ar(0)]),
						buf,
						sampleCount,
						0
					) <! write
				)
			}
		};

		zeroXBuf.do { |buf, i| write = (write <! BufWr.ar(zeroXPositions[i], buf, zeroXCount[i], 0)) };

		gate = 1 - ((sndBuf.collect { |b| BufFrames.ir(b) < sampleCount }).reduce('*'));
		EnvGen.ar(Env.asr(0, 1, 0), gate, doneAction: doneAction);

		// allows to ensure buffer writing before x in SynthDef with ZeroXBufWr <! x
		write <! out;

		^out
	}
}

