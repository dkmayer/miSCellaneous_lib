
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


// this code now looks straight, but it was hard to figure out ...
// taking the correct stutterings is the trick


// weights accept array of drate ugens
// currently this handles the case which is needed for DIdev,
// but not an arbitrary input of possibly nested drate ugens,
// therefore only recommended for use in this context

Dwrand_DIdev : DUGen {
	*new { |list, weights, normalize = 0, repeats = 1|
		var partialWeights, index = 0, sum = 1, dwhite;

		(normalize != 0).if {
			sum = 0;
			// weights have to be stuttered selectively as weights[list.size-1] won't be needed below
			weights = weights.collect { |w, i| Dstutter((i + 1 < list.size).if { 2 }{ 1 }, w) };
			list.size.do { |i| sum = sum + Dswitch1(weights, i) };
		};

		dwhite = Dstutter(list.size - 1, Dwhite(0, sum, repeats));

		partialWeights = 0 ! (list.size - 1);
		partialWeights[0] = Dstutter(list.size - 1, Dswitch1(weights, 0));
		(list.size - 2).do { |i|
			partialWeights[i+1] = Dstutter(list.size - 2 - i, Dswitch1(weights, i+1)) + partialWeights[i];
		};

		(list.size - 1).do { |i| index = index + (dwhite > partialWeights[i]) };

		^Dswitch1(list, index)
	}
}



// similar trigger tricks

// triggering difs triggers (maxLookBack * src, maxLookBack * phase)
// triggering probs triggers (maxHiDev - maxLoDev + 1) * difs

// triggering Dbufwr in addition triggers src and phase, so all in all we need
// maxLookBack * maxSpan + 1 as stutter number for src and phase
// whereas maxSpan = (maxHiDev - maxLoDev + 1)


DIdev : DUGen {

	*new { |in = 0, maxLookBack = 5, minLoDev = -5, maxHiDev = 5, lookBack, loDev, hiDev, thr = 1e-3, length = inf|

		var buf = LocalBuf(maxLookBack + 1).clear.set(inf!(maxLookBack + 1)), add,
			phase, src, dif, difs, probs, wr, maxSpan = maxHiDev - minLoDev + 1,
			indicatorLoDev = 1, indicatorHiDev = 1, sel;

		src = Dstutter(maxLookBack * maxSpan + 1, in);
		phase = Dstutter(maxLookBack * maxSpan + 1, Dseries(0));

		loDev.isKindOf(DUGen).if { loDev = Dstutter(maxSpan, loDev) };
		hiDev.isKindOf(DUGen).if { hiDev = Dstutter(maxSpan, hiDev) };
		lookBack.isKindOf(DUGen).if { lookBack = Dstutter(maxLookBack * maxSpan, lookBack) };

		// buffer is rewritten cyclically, so look back like this
		difs = { |i| Dbufrd(buf, phase - i - 1 % (maxLookBack + 1)) - src } ! maxLookBack;

		// for each index between bounds this indicates if there's an occurrence in the past,
		// if yes then probs is set to 0

		probs = { |i|
			var ind = 0;
			// consider dynamic lookBack here
			maxLookBack.do { |j| ind = ind + (((difs[j] - minLoDev - i).abs < thr) *
				(lookBack.isNil.if {
					1
				}{
					sel = UGen.miSC_methodSelectorForRate(lookBack.rate);
					(sel == \ar).if { SimpleInputError("no audio rate input allowed as lookBack arg").throw };
					DC.perform(sel, j) < lookBack
				}))
			};
			1 - ind.sign
		} ! maxSpan;

		// generate indicator arrays if certain -- possibly dynamic -- deviations are given
		loDev.notNil.if {
			sel = UGen.miSC_methodSelectorForRate(loDev.rate);
			indicatorLoDev = { |i| loDev <= DC.perform(sel, i + minLoDev) } ! maxSpan
		};
		hiDev.notNil.if {
			sel = UGen.miSC_methodSelectorForRate(hiDev.rate);
			indicatorHiDev = { |i| DC.perform(sel, i + minLoDev) <= hiDev } ! maxSpan
		};

		probs = (probs * indicatorLoDev * indicatorHiDev);

		add = Dwrand_DIdev((minLoDev..maxHiDev), probs, 1, length);

		^Dbufwr(src + add, buf, phase);
	}
}

