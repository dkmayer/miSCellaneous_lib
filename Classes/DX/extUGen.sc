
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



// this provides the newer version of lincurve for older SC versions with DX UGens
// Credits to Tim Blechmann and Julian Rohrhuber


+ UGen {
	lincurve_3_9 { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip = \minmax;
		var grow, a, b, scaled, curvedResult;
		if (curve.isNumber and: { abs(curve) < 0.125 }) {
			^this.linlin(inMin, inMax, outMin, outMax, clip)
		};
		grow = exp(curve);
		a = outMax - outMin / (1.0 - grow);
		b = outMin + a;
		scaled = (this.prune(inMin, inMax, clip) - inMin) / (inMax - inMin);

		curvedResult = b - (a * pow(grow, scaled));

		if (curve.rate == \scalar) {
			^curvedResult
		} {
			^Select.perform(this.methodSelectorForRate, abs(curve) >= 0.125, [
				this.linlin(inMin, inMax, outMin, outMax, clip),
				curvedResult
			])
		}
	}
}

+ AbstractFunction {
	lincurve_3_9 { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip = \minmax;
		^this.composeNAryOp('lincurve_3_9', [inMin, inMax, outMin, outMax, curve, clip])
	}
}

+ SequenceableCollection {
	lincurve_3_9 { arg ... args; ^this.multiChannelPerform('lincurve_3_9', *args) }
}

+ SequenceableCollection {
	lincurve_3_9 { arg ... args; ^this.multiChannelPerform('lincurve_3_9', *args) }
}

+ SimpleNumber {
	lincurve_3_9 { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip = \minmax;
		var grow, a, b, scaled;
		switch(clip,
			\minmax, {
				if (this <= inMin, { ^outMin });
				if (this >= inMax, { ^outMax });
			},
			\min, {
				if (this <= inMin, { ^outMin });
			},
			\max, {
				if (this >= inMax, { ^outMax });
			}
		);
		if (abs(curve) < 0.001) {
			// If the value should be clipped, it has already been clipped (above).
			// If we got this far, then linlin does not need to do any clipping.
			// Inlining the formula here makes it even faster.
			^(this-inMin)/(inMax-inMin) * (outMax-outMin) + outMin;
		};

		grow = exp(curve);
		a = outMax - outMin / (1.0 - grow);
		b = outMin + a;
		scaled = (this - inMin) / (inMax - inMin);

		^b - (a * pow(grow, scaled));
	}
}

