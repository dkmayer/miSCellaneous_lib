
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


PIdev : Pattern {

	var <>pattern, <>maxLookBack = 3, <>loDev = -3, <>hiDev = 3,
		<>lookBack, <>thr = 1e-3, <>length = inf;

	*new { |pattern, maxLookBack = 3, loDev = -3, hiDev = 3,
		lookBack, thr = 1e-3, length = inf|
		^super.newCopyArgs(pattern, maxLookBack, loDev, hiDev,
			lookBack, thr, length)
	}

	embedInStream { arg inval;
		var outval, srcVal, lookBackVal, loDevVal, hiDevVal, thrVal, count = 0,
			difs, devs, probs, add;
		var buf = inf ! (maxLookBack + 1);
		var srcStr = pattern.asStream;
		var lookBackStr = lookBack.asStream;
		var loDevStr = loDev.asStream;
		var hiDevStr = hiDev.asStream;
		var thrStr = thr.asStream;

		length.value(inval).do {
			srcVal = srcStr.next(inval);
			if(srcVal.isNil) { ^inval };

			lookBackVal = lookBackStr.next(inval) ?? { maxLookBack };
			loDevVal = loDevStr.next(inval);
			hiDevVal = hiDevStr.next(inval);
			thrVal = thrStr.next(inval);

			// this looks a bit different than in DIdev

			devs = (loDevVal.ceil..hiDevVal.floor);

			probs = devs.collect { |dev|
				var j = 0, prob = 1, dif;
				while { j < lookBackVal }{
					dif = buf[count - j - 1 % (maxLookBack + 1)] - srcVal - dev;
					(dif.abs <= thrVal).if { j = lookBackVal; prob = 0 };
					j = j + 1
				};
				prob
			};

			add = devs.wchoose(probs.normalizeSum);

			outval = srcVal + add;
			buf[count % (maxLookBack + 1)] = outval;

			count = count + 1;
			inval = outval.yield;
		};
		^inval;
	}

	storeArgs { ^[pattern, maxLookBack, loDev, hiDev, lookBack, thr, length] }
}


PLIdev : PIdev {
	var <>envir;
	*new { |pattern, maxLookBack = 3, loDev = -3, hiDev = 3, lookBack, thr = 1e-3, length = inf, envir = \current|
		^super.new(PL(pattern, 1, inf, envir), maxLookBack, PL(loDev, 1, inf, envir), PL(hiDev, 1, inf, envir),
			PL(lookBack, 1, inf, envir), PL(thr, 1, inf, envir), PL(length, length, 1, envir)).envir_(envir)
	}
	storeArgs { ^[pattern, maxLookBack, loDev, hiDev, lookBack, thr, length, envir] }
}
