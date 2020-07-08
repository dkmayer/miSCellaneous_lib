
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


// SmoothFoldS uses sine smoothing SmoothClipS at corners

SmoothFoldS : UGen {
	*ar { |in, lo = -1, hi = 1, foldRange = 1, smoothAmount = 0.5, delta = 0.00001|
		^this.multiNew(\audio, in, lo, hi, foldRange, smoothAmount, delta)
	}

	*kr { |in, lo = -1, hi = 1, foldRange = 1, smoothAmount = 0.5, delta = 0.00001|
		^this.multiNew(\control, in, lo, hi, foldRange, smoothAmount, delta)
	}

	*new1 { |rate, in, lo = -1, hi = 1, foldRange = 1, smoothAmount = 0.5, delta = 0.00001|
		var case, foldRangeAbs, thr_1, thr_2, selector = this.methodSelectorForRate(rate);

		// case = 0 for inSig in [lo, hi], 1 for inSig < lo and 2 for inSig > hi
		case = (in < lo) + (in > hi * 2);

		// thr_1 is the upper limit of the lower fold range
		// thr_2 is the lower limit of the upper fold range
		// thr_2 can be smaller than thr_1

		foldRangeAbs = (hi - lo) * foldRange;
		thr_1 = lo + foldRangeAbs;
		thr_2 = hi - foldRangeAbs;

		^Select.perform(selector, case, [
			SmoothClipS.perform(selector, in, lo, hi, smoothAmount, delta),
			SmoothClipS.perform(selector, Fold.perform(selector, in, lo, thr_1), lo, thr_1, smoothAmount, delta),
			SmoothClipS.perform(selector, Fold.perform(selector, in, thr_2, hi), thr_2, hi, smoothAmount, delta)
		]);
	}
}

// variant with two border ranges

SmoothFoldS2 : UGen {
	*ar { |in, lo = -1, hi = 1, foldRangeLo = 1, foldRangeHi = 1, smoothAmount = 0.5, delta = 0.00001|
		^this.multiNew(\audio, in, lo, hi, foldRangeLo, foldRangeHi, smoothAmount, delta)
	}

	*kr { |in, lo = -1, hi = 1, foldRangeLo = 1, foldRangeHi = 1, smoothAmount = 0.5, delta = 0.00001|
		^this.multiNew(\control, in, lo, hi, foldRangeLo, foldRangeHi, smoothAmount, delta)
	}

	*new1 { |rate, in, lo = -1, hi = 1, foldRangeLo = 1, foldRangeHi = 1, smoothAmount = 0.5, delta = 0.00001|
		var case, rangeAbs, thr_1, thr_2, selector = this.methodSelectorForRate(rate);

		// case = 0 for inSig in [lo, hi], 1 for inSig < lo and 2 for inSig > hi
		case = (in < lo) + (in > hi * 2);

		// thr_1 is the upper limit of the lower fold range
		// thr_2 is the lower limit of the upper fold range
		// thr_2 can be smaller than thr_1

		rangeAbs = hi - lo;
		thr_1 = lo + (rangeAbs * foldRangeLo);
		thr_2 = hi - (rangeAbs * foldRangeHi);

		^Select.perform(selector, case, [
			SmoothClipS.perform(selector, in, lo, hi, smoothAmount, delta),
			SmoothClipS.perform(selector, Fold.perform(selector, in, lo, thr_1), lo, thr_1, smoothAmount, delta),
			SmoothClipS.perform(selector, Fold.perform(selector, in, thr_2, hi), thr_2, hi, smoothAmount, delta)
		]);
	}
}


// SmoothFoldQ uses quadratic smoothing SmoothClipQ at corners

SmoothFoldQ : UGen {
	*ar { |in, lo = -1, hi = 1, foldRange = 1, smoothAmount = 0.5, delta = 0.00001|
		^this.multiNew(\audio, in, lo, hi, foldRange, smoothAmount, delta)
	}

	*kr { |in, lo = -1, hi = 1, foldRange = 1, smoothAmount = 0.5, delta = 0.00001|
		^this.multiNew(\control, in, lo, hi, foldRange, smoothAmount, delta)
	}

	*new1 { |rate, in, lo = -1, hi = 1, foldRange = 1, smoothAmount = 0.5, delta = 0.00001|
		var case, foldRangeAbs, thr_1, thr_2, selector = this.methodSelectorForRate(rate);

		// case = 0 for inSig in [lo, hi], 1 for inSig < lo and 2 for inSig > hi
		case = (in < lo) + (in > hi * 2);

		// thr_1 is the upper limit of the lower fold range
		// thr_2 is the lower limit of the upper fold range
		// thr_2 can be smaller than thr_1

		foldRangeAbs = (hi - lo) * foldRange;
		thr_1 = lo + foldRangeAbs;
		thr_2 = hi - foldRangeAbs;

		^Select.perform(selector, case, [
			SmoothClipQ.perform(selector, in, lo, hi, smoothAmount, delta),
			SmoothClipQ.perform(selector, Fold.perform(selector, in, lo, thr_1), lo, thr_1, smoothAmount, delta),
			SmoothClipQ.perform(selector, Fold.perform(selector, in, thr_2, hi), thr_2, hi, smoothAmount, delta)
		]);
	}
}

// variant with two border ranges

SmoothFoldQ2 : UGen {
	*ar { |in, lo = -1, hi = 1, foldRangeLo = 1, foldRangeHi = 1, smoothAmount = 0.5, delta = 0.00001|
		^this.multiNew(\audio, in, lo, hi, foldRangeLo, foldRangeHi, smoothAmount, delta)
	}

	*kr { |in, lo = -1, hi = 1, foldRangeLo = 1, foldRangeHi = 1, smoothAmount = 0.5, delta = 0.00001|
		^this.multiNew(\control, in, lo, hi, foldRangeLo, foldRangeHi, smoothAmount, delta)
	}

	*new1 { |rate, in, lo = -1, hi = 1, foldRangeLo = 1, foldRangeHi = 1, smoothAmount = 0.5, delta = 0.00001|
		var case, rangeAbs, thr_1, thr_2, selector = this.methodSelectorForRate(rate);

		// case = 0 for inSig in [lo, hi], 1 for inSig < lo and 2 for inSig > hi
		case = (in < lo) + (in > hi * 2);

		// thr_1 is the upper limit of the lower fold range
		// thr_2 is the lower limit of the upper fold range
		// thr_2 can be smaller than thr_1

		rangeAbs = hi - lo;
		thr_1 = lo + (rangeAbs * foldRangeLo);
		thr_2 = hi - (rangeAbs * foldRangeHi);

		^Select.perform(selector, case, [
			SmoothClipQ.perform(selector, in, lo, hi, smoothAmount, delta),
			SmoothClipQ.perform(selector, Fold.perform(selector, in, lo, thr_1), lo, thr_1, smoothAmount, delta),
			SmoothClipQ.perform(selector, Fold.perform(selector, in, thr_2, hi), thr_2, hi, smoothAmount, delta)
		]);
	}
}
