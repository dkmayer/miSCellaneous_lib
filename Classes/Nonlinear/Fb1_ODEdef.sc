
/*
	This file is part of miSCellaneous, a program library for SuperCollider 3

	Created: 2020-04-19, version 0.23
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


Fb1_ODEdef {

	classvar all, initKeys;

	var <name, <function, <t0, <y0, <outScale, <diffOutScale, <size;

	*new { |name, function, t0 = 0, y0, outScale = 1, diffOutScale = 1|
		^super.newCopyArgs(
			name, function, t0, y0, outScale, diffOutScale
		).initFb1_ODEdef(name, y0);
	}

	*at { |key| ^all.at(key) }

	*keys { ^all.keys.copy }

	*remove { |key| initKeys.includes(key).not.if { all.put(key, nil) } }

	// remove all added Fb1_ODEdefs
	*reset { all.do { |x| this.remove(x.name) } }

	*postAll {
		var nameStrings = (this.keys.asArray.collect { |x| x.asString }).sort;
		"\nCurrent Fb1_ODEdefs: \n".postln;
		nameStrings.do { |str| str.postln; };
		"".postln;
	}

	*initClass {

		all = IdentityDictionary.new;

		// for symplectic integration it's a bit more efficient at SynthDef compile time
		// to pass Functions in the arrays as - in contrast to other integrators -
		// only single Functions are needed

		Fb1_ODEdef(\MSD, { |t, y, f = 0, mass = 1, spring = 1, dampen = 0|
			[
				{ y[1] },
				{ f - (dampen * y[1]) - (spring * y[0]) / mass }
			]
		}, 0, [0, 0], 1, 1);


		Fb1_ODEdef(\SD, { |t, y, f = 0, spring = 1, dampen = 0|
			[
				{ y[1] },
				{ f - (dampen * y[1]) - (spring * y[0]) }
			]
		}, 0, [0, 0], 1, 1);


		Fb1_ODEdef(\Lorenz, { |t, y, s = 10, r = 30, b = 2|
			[
				{ (y[1] - y[0]) * s },
				{ (r - y[2]) * y[0] - y[1] },
				{ (y[0] * y[1]) - (b * y[2]) }
			]
		}, 0, [1, 1, 1], 0.01, 0.01);


		Fb1_ODEdef(\Hopf, { |t, y, f = 0, mu = 1, theta = 1|
			var u, v;
			u = y[0] * y[0] + (y[1] * y[1]);
			v = mu - u;
			[
				{ v * y[0] - (theta * y[1]) + f },
				{ v * y[1] + (theta * y[0]) },
			]
		}, 0, [1, 1], 0.3, 0.3);


		// https://biorob.epfl.ch/research/research-dynamical/page-36365-en-html

		Fb1_ODEdef(\HopfA, { |t, y, f = 0, mu = 1, eta = 1|
			var u, v;
			u = y[0] * y[0] + (y[1] * y[1]);
			v = mu - u;
			[
				{ v * y[0] - (y[2] * y[1]) + f },
				{ v * y[1] + (y[2] * y[0]) },
				{ f.neg * y[1] * eta / sqrt(u) }
			]
		}, 0, [1, 1, 1], 0.3, 0.3);


		// https://www.frontiersin.org/articles/10.3389/fnbot.2017.00014/full

		Fb1_ODEdef(\HopfAFDC, { |t, y, f = 0, mu = 1,
			eta = 1, tau = 1, kappa = 1, beta0 = 0.01, epsilon0 = 0.01|
			var p, u, v;
			u = y[0] * y[0] + (y[1] * y[1]);
			v = mu - u;
			p = y[3] * f - (y[2] * y[0]);
			[
				{ v * y[0] - (y[4] * y[1]) + p },
				{ v * y[1] + (y[4] * y[0]) },
				{ beta0 - y[2] + (kappa * p * y[0]) / tau },
				{ epsilon0 - y[3] + (kappa * p * f) / tau },
				{ p.neg * y[1] * eta / sqrt(u) }
			]
		}, 0, [1, 1, 0.01, 0.01, 1], 0.3, 0.3);


		Fb1_ODEdef(\VanDerPol, { |t, y, f = 0, mu = 1, theta = 1|
			var y0s = y[0] * y[0];
			[
				{ y[1] },
				{ mu * (1 - y0s) * y[1] + f - (theta * theta * y[0]) }
			]
		}, 0, [1, 1], 0.05, 0.05);


		Fb1_ODEdef(\Duffing, { |t, y, f = 0, alpha = 0, beta = 1, gamma = 1, delta = 1,
			omega = 1|
			[
				{ y[1] },
				{ gamma * cos(omega * t) + f - (delta * y[1]) -
					((beta * y[0] * y[0] - alpha) * y[0]) }
			]
		}, 0, [1, 0], 1, 1);

		initKeys = all.keys;
	}

	initFb1_ODEdef { |name, y0|

		(initKeys.notNil and: { initKeys.includes(name) }).if {
			var err = SimpleInitError("Fb1_ODEdef of this name already exists");
			err.class_("Fb1_ODEdef");
			err.throw;
		};

		size = y0.asArray.size;
		all.put(name, this)
	}

	// needed for Fb1_ODEintdefs:
	// for symplectic procedures the single functional components are used

	value { |i ... args|
		^i.isNil.if {
			// for dim = 1 no need to pass ODE (Function) in array
			function.(*args).asArray.collect(_.value)
		}{
			function.(*args).miSC_maybeWrapAt(i).value
		}
	}

	// this composes the ODE and the integration method to the
	// stepper Function to be used in Fb1

	compose { |intType|
		^{ |t, y, dt, size ... args|
			Fb1_ODEintdef.at(intType).function.(this, t, y, dt, size, *args)
		}
	}


	// methods for numerical integration in language
	// esp. needed for start of multi-step integration with Fb1
	// not designed for multi-step integration in language itself !

	next { |intType, t, y, dt, args|
		var intSize = this.getIntSize(intType);
		y = y.asArray;
		^this.compose(intType).(t, y[0..intSize-1], dt, size, *args)
	}


	// produces a nested array of integration values
	// last value/array is last solution

	nextN { |n, intType, t, y, dt, args, withTime = false, includeStart = true|
		var z = Array.newClear(n+1), oldT, zLast;
		var intSize = this.getIntSize(intType);

		z[0] = ((size == intSize).if {
			y
		}{
			this.makeFirstOutInit(intType, t, y, args, false)
		}).asArray;

		withTime.if { z[0] = z[0] ++ t };
		n.do { |i|
			oldT = dt * i + t; // better multiplying than summing up
			zLast = withTime.if { z[i].drop(-1) }{ z[i] };
			z[i+1] = this.next(intType, oldT, zLast, dt, args) ++
				withTime.if { oldT + dt };
		};
		includeStart.not.if { z = z.drop(1) };
		^z
	}

	// depending on fb1_intType we need to calculate
	// the function value for the start y0

	makeFirstOutInit { |fb1_intType, t0, y0, args, withTime|
		var z0 = y0;
		(Fb1_ODEintdef.at(fb1_intType).sizeFactor > 1).if {
			z0 = y0 ++ this.(nil, t0, y0, *args);
		};
		withTime.if { z0 = z0 ++ [t0] };
		^z0
	}

	makeOutInit { |fb1_intType, init_intType = \sym8, t0, y0, dt, args, withTime = true|
		var z0 = this.makeFirstOutInit(fb1_intType, t0, y0, args, withTime);
		var depth = Fb1_ODEintdef.at(fb1_intType).stepDepth;

		z0 = (depth == 1).if {
			[z0]
		}{
			// produces startValues for multi-step procedures like
			// Adams-Bashforth und Adams-Moulton
			this.nextN(depth - 1, init_intType, t0, z0, dt, args, withTime, true)
		};
		// the result is reversed (last values first) and flopped
		// in order to be prepared as Fb1 outInit arg
		^z0.reverse.flop
	}

	getIntSize { |intType|
		^size * Fb1_ODEintdef.at(intType).sizeFactor;
	}

	dispatchIntFunc { |intType|
		var multiStep = Fb1_ODEintdef.at(intType).stepDepth > 1;
		var n = this.getIntSize(intType);
		var comp = this.compose(intType); // step Function from ODE and intType
		var size = this.size;

		// this Function is used inside Fb1's Function
		// z gets the 'out' arg

		// oldT comes from the external time integrator (Sweep),
		// which is necessary to avoid drifts that occur with
		// plain summation via binary operators

		^{ |z, oldT, dt, argList|
			var y;
			// multi-step integration: need previous arrays from Fb1 'out'
			// one-step integration: need only one previous array from Fb1 'out'
			multiStep.if { y = z.drop(1) }{ y = z[1] };
			comp.(oldT, y, dt, size, *argList);
		}
	}

}

