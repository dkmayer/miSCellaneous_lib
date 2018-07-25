
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


// Dynamic waveshaping with line and sine fragment:

// As math is easier for the case lo == -1, hi == +1,
// we regard this case first and do linear mapping / remapping for other bounds:
// Take tangent through zero for abs(x) < given tangent point x0
// and sine fragment values above x0 and below 1, clip for abs values above 1.
// The transfer function is antisymmetrical, its slope tends to 0 for x -> +-1.
// Source and target range can be shifted with fromLo, fromHi, toLo, toHi.

// The sine fragment with minimum at x = 1 and f(1) = 1 has the form
// f(x) = p sin(pi/2 x) - p + 1
// f'(x) = p pi/2 cos(pi/2 x)

// Thus the tangent through x0 has the form
// p pi/2 cos(pi/2 x0) x = p sin(pi/2 x0) - p + 1

// So for a given tangent point 0 <= x0 < 1 we can calculate p as below.

// 'amount' indicates the amount of smoothing:
// amount == 0: in signal isn't altered
// amount == 1: in signal is shaped by full sine fragment
// amount expected to be >= 0 and <= 1


SmoothClipS : UGen {

	*ar { |in, lo = -1, hi = 1, amount = 0.5, delta = 0.00001|
		^this.multiNew(\audio, in, lo, hi, amount, delta)
	}

	*kr { |in, lo = -1, hi = 1, amount = 0.5, delta = 0.00001|
		^this.multiNew(\control, in, lo, hi, amount, delta)
	}

	*new1 { |rate, in, lo = -1, hi = 1, amount = 0.5, delta = 0.00001|
		var w, p, x0, slope, case, realSmooth, linSig, sineSig,
			fromFactor, fromOffset, dif, sum, selector = this.methodSelectorForRate(rate);

		// map to unit range
		dif = hi - lo;
		sum = hi + lo;
		// avoid zero division with lo = hi
		dif = max(dif.abs, delta) * ((dif >= DC.perform(selector, 0)) * 2 - 1);
		fromFactor = 2 / dif;
		fromOffset = fromFactor * hi.neg + 1;

		in = fromFactor * in + fromOffset;

		// this excludes the border case p -> inf for x0 -> 1
		x0 = min(1 - amount, 1 - delta);

		w = x0 * pi * 0.5;
		p = 1 / (1 - sin(w) + (w * cos(w)));
		slope = p * pi * 0.5 * cos(w);
		linSig = slope * in;
		sineSig = sin(in.abs * pi * 0.5) - 1 * p + 1 * in.sign;

		// distinct and remap to passed range
		case = (in.abs > x0) + (in.abs >= 1) + (in <= -1);
		^Select.perform(selector, case, [
			linSig,
			sineSig,
			DC.perform(selector, 1),
			DC.perform(selector, -1)
		] * dif + sum * 0.5);
	}
}


// Dynamic waveshaping with quadratic polynomial.

// As math is easier for the case lo == -1, hi == +1,
// we regard this case first and do linear mapping / remapping for other bounds:
// Take tangent through zero for abs(x) < given tangent point x0
// and parable values above x0 and below 1, clip for abs values above 1.
// The transfer function is antisymmetrical, its slope tends to 0 for x -> +-1.

// 'amount' indicates the amount of smoothing:
// amount == 0: in signal isn't altered
// amount == 1: in signal is shaped by full parable fragment
// amount expected to be >= 0 and <= 1

SmoothClipQ : UGen {
	*ar { |in, lo = -1, hi = 1, amount = 0.5, delta = 0.00001|
		^this.multiNew(\audio, in, lo, hi, amount, delta)
	}

	*kr { |in, lo = -1, hi = 1, amount = 0.5, delta = 0.00001|
		^this.multiNew(\control, in, lo, hi, amount, delta)
	}

	*new1 { |rate, in, lo = -1, hi = 1, amount = 0.5, delta = 0.00001|
		var p, x0, slope, realSmooth, linSig, parableSig, case,
			fromFactor, fromOffset, dif, sum, selector = this.methodSelectorForRate(rate);

		// map to unit range
		dif = hi - lo;
		sum = hi + lo;
		// avoid zero division with lo = hi
		dif = max(dif.abs, delta) * ((dif >= DC.perform(selector, 0)) * 2 - 1);
		fromFactor = 2 / dif;
		fromOffset = fromFactor * hi.neg + 1;

		in = fromFactor * in + fromOffset;

		// this excludes the border case p -> inf for x0 -> 1
		x0 = min(1 - amount, 1 - delta);

		p = 1 / (x0 * x0 - 1);
		slope = 2 * p * (x0 - 1);

		linSig = slope * in;
		parableSig = (in.abs - 1).squared * p + 1 * in.sign;

		// distinct and remap to passed range
		case = (in.abs > x0) + (in.abs >= 1) + (in <= -1);
		^Select.perform(selector, case, [
			linSig,
			parableSig,
			DC.perform(selector, 1),
			DC.perform(selector, -1)
		] * dif + sum * 0.5);
	}
}

